/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2017 Jahia Solutions Group SA. All rights reserved.
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
