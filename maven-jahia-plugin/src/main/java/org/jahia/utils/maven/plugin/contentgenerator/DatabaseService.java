/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2019 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
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
