/**
 * This file is part of Jahia, next-generation open source CMS:
 * Jahia's next-generation, open source CMS stems from a widely acknowledged vision
 * of enterprise application convergence - web, search, document, social and portal -
 * unified by the simplicity of web content management.
 *
 * For more information, please visit http://www.jahia.com.
 *
 * Copyright (C) 2002-2014 Jahia Solutions Group SA. All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * As a special exception to the terms and conditions of version 2.0 of
 * the GPL (or any later version), you may redistribute this Program in connection
 * with Free/Libre and Open Source Software ("FLOSS") applications as described
 * in Jahia's FLOSS exception. You should have received a copy of the text
 * describing the FLOSS exception, and it is also available here:
 * http://www.jahia.com/license
 *
 * Commercial and Supported Versions of the program (dual licensing):
 * alternatively, commercial and supported versions of the program may be used
 * in accordance with the terms and conditions contained in a separate
 * written agreement between you and Jahia Solutions Group SA.
 *
 * If you are unsure which license is appropriate for your use,
 * please contact the sales department at sales@jahia.com.
 */
package org.jahia.izpack;

import java.text.Collator;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import com.izforge.izpack.installer.AutomatedInstallData;
import com.izforge.izpack.panels.ProcessingClient;
import com.izforge.izpack.panels.Processor;

/**
 * Choice list initializer for the avilable locales.
 * 
 * @author Sergiy Shyrkov
 */
public class LanguageChoiceProcessor implements Processor, Processor.AutomatedInstallDataAware {

    /**
     * Comparator implementation that compares locale display names in a certain current locale.
     */
    private static class LocaleDisplayNameComparator implements Comparator<Locale> {

        private Collator collator = Collator.getInstance();
        private Locale currentLocale;

        public LocaleDisplayNameComparator(Locale locale) {
            if (locale != null) {
                this.currentLocale = locale;
                collator = Collator.getInstance(locale);
            }
        }

        public int compare(Locale locale1, Locale locale2) {
            return collator.compare(locale1.getDisplayName(currentLocale), locale2.getDisplayName(currentLocale));
        }

        public boolean equals(Object obj) {
            if (obj instanceof LocaleDisplayNameComparator) {
                return true;
            } else {
                return false;
            }
        }
    }

    private static String capitalize(String str) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return str;
        }
        return new StringBuilder(strLen).append(Character.toTitleCase(str.charAt(0))).append(str.substring(1))
                .toString();
    }

    private static List<Locale> getSortedLocaleList(Locale currentLocale) {
        List<Locale> sortedLocaleList = Arrays.asList(Locale.getAvailableLocales());
        Collections.sort(sortedLocaleList, new LocaleDisplayNameComparator(currentLocale));
        return sortedLocaleList;
    }

    private AutomatedInstallData idata;

    @Override
    public String process(ProcessingClient client) {
        Locale displayLocale = idata.locale != null ? idata.locale : Locale.ENGLISH;
        StringBuilder text = new StringBuilder();
        for (Locale l : getSortedLocaleList(displayLocale)) {
            if (text.length() > 0) {
                text.append(':');
            }
            text.append(l).append('|').append(capitalize(l.getDisplayName(displayLocale)));
        }
        return text.toString();
    }

    @Override
    public void setAutomatedInstallData(AutomatedInstallData idata) {
        this.idata = idata;
    }

}
