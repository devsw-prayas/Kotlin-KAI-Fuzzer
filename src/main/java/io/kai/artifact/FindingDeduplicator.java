package io.kai.artifact;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class FindingDeduplicator {

    private final Set<String> seen = Collections.synchronizedSet(new HashSet<>());

    /**
     * Returns true if this is a new unique finding.
     * Returns false if we've seen this stack trace signature before.
     */
    public boolean isNew(String stderr) {
        String signature = extractSignature(stderr);
        return seen.add(signature);
    }

    /**
     * Extracts a signature from stderr by taking the first 3 kotlinc stack frames
     * and hashing their concatenation.
     */
    private String extractSignature(String stderr) {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (String line : stderr.lines().toList()) {
            String trimmed = line.trim();
            if (trimmed.startsWith("at org.jetbrains.kotlin.")) {
                sb.append(trimmed).append("|");
                count++;
                if (count >= 3) break;
            }
        }
        // If no kotlin frames found, use first 200 chars of stderr as signature
        if (sb.isEmpty()) {
            return stderr.substring(0, Math.min(200, stderr.length()));
        }
        return Integer.toHexString(sb.toString().hashCode());
    }

    public int uniqueCount() { return seen.size(); }
}
