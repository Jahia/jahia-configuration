/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2023 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.init;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import com.izforge.izpack.util.AbstractUIProcessHandler;

/**
 * <p>Title: Tomcat waiting utility</p>
 * <p>Description: Small standalone utility to wait until the Tomcat web server
 * has finished initializing before going on with the startup script.</p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author Serge Huber
 * @version 1.0
 */

public class TomcatWait {
    private static final String DEFAULT_TARGET_URL = "http://localhost:8080/cms/html/startup/startjahia.html";
    private static final long DEFAULT_WAIT_TIMEOUT = 180000L;

    public static void main(String args[]) throws MalformedURLException {
        execute(args.length == 0 ? DEFAULT_TARGET_URL : args[0], args.length < 2 ? DEFAULT_WAIT_TIMEOUT : Long.valueOf(args[1]));
    }

    /**
     * @param handler The UI handler for user interaction and to send output to.
     * @return true on success, false if processing should stop
     */
    public boolean run(AbstractUIProcessHandler handler, String[] args) {
        try {
            execute(args.length == 0 ? DEFAULT_TARGET_URL : args[0], args.length < 2 ? DEFAULT_WAIT_TIMEOUT : Long.valueOf(args[1]));
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private static void execute(String pingUrl, long timeout) throws MalformedURLException {
        URL url = new URL(pingUrl);

        boolean flag = false;
        System.out.print("Waiting for Web Server to become available at " + url);
        
        long time = 0;

        try {
        	time+=1000;
            Thread.sleep(1000); // sleep 1 second before making the connection
        } catch (InterruptedException ie) {
            // ignore it
        }
        while (!flag && time <= timeout) {
            System.out.print(".");
            try {
            	time+=500;
                Thread.sleep(500); // sleep 500 ms between tries.
            } catch (InterruptedException ie) {
                // ignore it
            }

            try {
                InputStream inputstream = url.openStream();
                flag = true;
                inputstream.close();
            } catch (Exception throwable1) {
                flag = false;
            }
        }
        System.out.println(flag ? "\nWeb Server now available." : "\nTimeout waiting for Web Server startup.");
    }
}