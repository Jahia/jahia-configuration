package org.jahia.configuration.cluster.tool.operations;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import org.jahia.configuration.cluster.ClusterConfigBean;
import org.jahia.configuration.logging.AbstractLogger;

import java.io.IOException;
import java.sql.*;

/**
 * This operation will update the local revision of each node
 */
public class ClusterUpdateLocalRevisions extends AbstractClusterOperation {

    private boolean forceIncrease = false;

    public ClusterUpdateLocalRevisions(AbstractLogger logger, ClusterConfigBean clusterConfigBean) {
        super(logger, clusterConfigBean);
    }

    @Override
    public void execute() throws JSchException, SftpException, IOException {
        try {
            Class.forName(clusterConfigBean.getDbDriverClass());
        } catch (ClassNotFoundException e) {
            logger.error("Error loading database driver " + clusterConfigBean.getDbDriverClass() + ". Aborting process.", e);
            return;
        }
        Connection dbConnection = null;
        try {
            dbConnection = DriverManager.getConnection(clusterConfigBean.getDbUrl(), clusterConfigBean.getDbUser(), clusterConfigBean.getDbPassword());
        } catch (SQLException e) {
            logger.error("Error connecting to the database. Aborting process", e);
            return;
        }

        try {
            dbConnection.setAutoCommit(false);
            PreparedStatement maxRevisionStatement = dbConnection.prepareStatement("SELECT max(REVISION_ID) FROM " + clusterConfigBean.getDbLocalRevisionsTableName());
            ResultSet maxRevisionResultSet = maxRevisionStatement.executeQuery();
            long maximumRevisionId = -1;

            while (maxRevisionResultSet.next()) {
                maximumRevisionId = maxRevisionResultSet.getLong(1);
            }
            maxRevisionResultSet.close();
            maxRevisionStatement.close();

            if (maximumRevisionId == -1) {
                logger.error("Couldn't find maximum revision ID, aborting... ");
                return;
            }

            logger.info("Maximum revision ID found=" + maximumRevisionId);

            PreparedStatement getCurrentRevisionStatement = dbConnection.prepareStatement("SELECT REVISION_ID FROM " + clusterConfigBean.getDbLocalRevisionsTableName() + " WHERE JOURNAL_ID=?");
            PreparedStatement updateRevisionStatement = dbConnection.prepareStatement("UPDATE " + clusterConfigBean.getDbLocalRevisionsTableName() + " SET REVISION_ID=? WHERE JOURNAL_ID=?");
            PreparedStatement insertRevisionStatement = dbConnection.prepareStatement("INSERT INTO " + clusterConfigBean.getDbLocalRevisionsTableName() + " (JOURNAL_ID, REVISION_ID) VALUES (?,?)");
            for (int i = 0; i < clusterConfigBean.getNumberOfNodes(); i++) {

                String nodeId = clusterConfigBean.getNodeNamePrefix() + Integer.toString(i+1);

                getCurrentRevisionStatement.setString(1, nodeId);

                ResultSet curRevisionResultSet = getCurrentRevisionStatement.executeQuery();
                if (curRevisionResultSet.next()) {
                    // we found one, let's update it if it is lower.
                    long curRevisionId = curRevisionResultSet.getLong(1);
                    if (curRevisionId < maximumRevisionId) {
                        if (forceIncrease) {
                            logger.info("Updating existing revision ID " + curRevisionId + " for node " + nodeId + " to value " + maximumRevisionId);
                            updateRevisionStatement.setLong(1, maximumRevisionId);
                            updateRevisionStatement.setString(2, nodeId);
                            int result = updateRevisionStatement.executeUpdate();
                        } else {
                            logger.info("Found lower existing revision ID " + curRevisionId + " for node " + nodeId + ", will not modify it.");

                        }
                    } else {
                        logger.info("Existing revision ID " + curRevisionId + " for node " + nodeId + " is fine, will not modify it.");

                    }
                } else {
                    logger.info("Creating new revision ID " + maximumRevisionId + " for node " + nodeId);
                    insertRevisionStatement.setString(1, nodeId);
                    insertRevisionStatement.setLong(2, maximumRevisionId);
                    int result = insertRevisionStatement.executeUpdate();
                }
            }
            dbConnection.commit();
        } catch (SQLException e) {
            logger.error("Error updating revision number for all cluster nodes", e);
        } finally {
            try {
                dbConnection.close();
            } catch (SQLException e) {
                logger.error("Error closing connection", e);
            }
        }
    }
}
