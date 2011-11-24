package org.jahia.configuration.cluster.tool.operations;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import org.jahia.configuration.cluster.tool.ClusterConfigBean;
import org.jahia.configuration.logging.AbstractLogger;

import java.io.IOException;
import java.util.List;

/**
 * Small helper class to quickly retrieve a list of EC2 instances, their public DNS names and internals IPs to avoid
 * copy-pasting the list by hand.
 */
public class AWSGetClusterInstances extends AbstractClusterOperation {

    public AWSGetClusterInstances(AbstractLogger logger, ClusterConfigBean clusterConfigBean) {
        super(logger, clusterConfigBean);
    }

    @Override
    public void execute() throws JSchException, SftpException, IOException {
        BasicAWSCredentials basicAWSCredentials = new BasicAWSCredentials(clusterConfigBean.getAwsAccessKey(), clusterConfigBean.getAwsSecretKey());
        AmazonEC2Client amazonEC2Client = new AmazonEC2Client(basicAWSCredentials);
        DescribeInstancesResult describeInstancesResult = amazonEC2Client.describeInstances();
        StringBuffer publicDnsNames = new StringBuffer();
        StringBuffer privateIpAddresses = new StringBuffer();
        if (describeInstancesResult != null) {
            List<Reservation> reservationsList = describeInstancesResult.getReservations();
            for (Reservation reservation : reservationsList) {
                List<Instance> instanceList = reservation.getInstances();
                for (Instance instance: instanceList) {
                    publicDnsNames.append(instance.getPublicDnsName());
                    publicDnsNames.append(",");
                    privateIpAddresses.append(instance.getPrivateIpAddress());
                    privateIpAddresses.append(",");
                }
            }
        }
        logger.info("Public DNS names = " + publicDnsNames.toString());
        logger.info("Private IP addresses = " + privateIpAddresses.toString());
    }
}
