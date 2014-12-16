package org.jahia.utils.maven.plugin.contentgenerator;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.jahia.utils.maven.plugin.contentgenerator.bo.ArticleBO;
import org.jahia.utils.maven.plugin.contentgenerator.bo.ExportBO;
import org.jahia.utils.maven.plugin.contentgenerator.properties.ContentGeneratorCst;
import org.jahia.utils.maven.plugin.contentgenerator.properties.DatabaseProperties;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class DatabaseService {
	private static DatabaseService instance;

	private static Connection dbConnection;

	private DatabaseService() {
	}

    private static final Log logger = new SystemStreamLog();


    public static DatabaseService getInstance() {
		if (instance == null) {
			instance = new DatabaseService();
		}
		return instance;
	}

	public Connection getConnection() {
		if (dbConnection == null) {
			try {
				Class.forName("com.mysql.jdbc.Driver").newInstance();

				StringBuffer sbConnection = new StringBuffer("jdbc:mysql://");
				sbConnection.append(DatabaseProperties.HOSTNAME)
						.append(":").append(DatabaseProperties.PORT)
						.append("/")
						.append(DatabaseProperties.DATABASE)
						.append("?user=")
						.append(DatabaseProperties.USER)
						.append("&password=")
						.append(DatabaseProperties.PASSWORD);
				logger.info("Connecting to " + sbConnection);

				dbConnection = java.sql.DriverManager.getConnection(sbConnection.toString());

				logger.info("MySQL connection established.");
			} catch (InstantiationException e) {
				logger.error("Error during MySQL connection instantiation", e);
			} catch (IllegalAccessException e) {
				logger.error("Error during MySQL connection", e);
			} catch (ClassNotFoundException e) {
				logger.error("Error during MySQL connection instantiation", e);
			} catch (SQLException e) {
				logger.error("Error during MySQL connection instantiation", e);
			}
		}
		return dbConnection;
	}

	public void closeConnection() {
		if (null != dbConnection) {
			try {
				dbConnection.close();
                dbConnection = null;
			} catch (SQLException e) {
				logger.error("Error during connection close", e);
			}
		}
	}

	public List<ArticleBO> selectArticles(ExportBO export, Integer numberOfArticles) {
		Integer querySize = getRecordSetsSize(export, numberOfArticles);
		List<Integer> articlesId;
		List<ArticleBO> articlesContent = new ArrayList<ArticleBO>();

		articlesId = getArticlesIds(querySize);

		try {
			articlesContent.addAll(getArticlesContent(articlesId));
		} catch (SQLException e) {
			logger.error("Error during articles content selection", e);
		}
		closeConnection();

		return articlesContent;
	}

	public List<Integer> getArticlesIds(Integer recordSetSize) {
		logger.debug("Selecting " + recordSetSize + " ID's from database");
		Statement stmt = null;
		List<Integer> idList = new ArrayList<Integer>();

		try {
			stmt = this.getConnection().createStatement();

			StringBuffer sbQuery = new StringBuffer("SELECT a.id_article FROM ");
			sbQuery.append(DatabaseProperties.TABLE + " a ");
			sbQuery.append(" ORDER BY RAND() ");
			sbQuery.append(" LIMIT 0," + recordSetSize);

			logger.debug("SQL Query: " + sbQuery.toString());
			boolean resultStmt = stmt.execute(sbQuery.toString());
			ResultSet results = null;

			if (resultStmt) {
				results = stmt.getResultSet();
				while (results.next()) {
					idList.add(results.getInt("id_article"));
				}
			}

		} catch (SQLException e) {
			logger.error("Error while requesting articles ID", e);
		}

		return idList;
	}

	public List<ArticleBO> getArticlesContent(final List<Integer> articlesId) throws SQLException {
		logger.info("Selecting " + articlesId.size() + " record(s) from database");

		Statement stmt = null;

		stmt = this.getConnection().createStatement();
		StringBuffer sbIdCommaSeparated = new StringBuffer();
		for (Iterator<Integer> iterator = articlesId.iterator(); iterator.hasNext();) {
			Integer id = (Integer) iterator.next();
			sbIdCommaSeparated.append(id + ",");
		}
		// removes trailing comma
		String idCommaSeparated = sbIdCommaSeparated.substring(0, sbIdCommaSeparated.length() - 1);

		StringBuffer sbQuery = new StringBuffer("SELECT a.id_article, a.title,a.content FROM ");
		sbQuery.append(DatabaseProperties.TABLE + " a ");
		sbQuery.append(" WHERE ");
		sbQuery.append(" a.id_article IN (" + idCommaSeparated + ")");

		logger.debug("SQL Query: " + sbQuery.toString());
		boolean resultStmt = stmt.execute(sbQuery.toString());
		ResultSet results = null;
		List<ArticleBO> articlesList = null;
		if (resultStmt) {
			results = stmt.getResultSet();
			articlesList = getArticleCollectionFromResultSet(results);
		}

		return articlesList;
	}

	private Integer getRecordSetsSize(ExportBO export, Integer numberOfArticles) {
		Integer totalRecords = null;
		if (numberOfArticles.compareTo(ContentGeneratorCst.SQL_RECORDSET_SIZE) < 0) {
			totalRecords = numberOfArticles;
		} else {
			totalRecords = ContentGeneratorCst.SQL_RECORDSET_SIZE;
		}
		export.setMaxArticleIndex(totalRecords-1);
		return totalRecords;
	}

	private List<ArticleBO> getArticleCollectionFromResultSet(final ResultSet articles) throws SQLException {
		List<ArticleBO> listeArticles = new ArrayList<ArticleBO>();

		Integer idArticle;
		String title;
		String content;
		ArticleBO article = null;
		while (articles.next()) {
			idArticle = articles.getInt("id_article");
			title = articles.getString("title");
			content = articles.getString("content");

			article = new ArticleBO(idArticle, title, content);
			listeArticles.add(article);
		}
		return listeArticles;
	}
}
