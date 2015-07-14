package org.jahia.utils.maven.plugin.support;

import java.util.concurrent.ThreadLocalRandom;

public class RandomUtils {

    public static boolean isRandomOccurrence(int pcLikelihood) {
        if (pcLikelihood < 0 || pcLikelihood > 100) {
            throw new IllegalArgumentException();
        }
        int pc = ThreadLocalRandom.current().nextInt(100 + 1);
        return (pc > 0 && pc <= pcLikelihood);
    }
}
