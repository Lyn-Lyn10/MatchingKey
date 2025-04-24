package src;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.*;

public class CandidateKeyGeneration {
    Map<String, int[]> distanceCache = new HashMap<>();

    public Set<Candidate> preprocessAndCalculateDistances(List<Tuple> r, double eta_c, Set<String> correctPairs, int min, int max,List<Double> ratios) {
        long startTimeMillis = System.currentTimeMillis();
        int totalPairs = (r.size() * (r.size() - 1)) / 2; 
        int numAttributes = r.get(0).size();
        double[] maxDistances = new double[numAttributes];
        Map<String, Integer> tempDistances = new HashMap<>();
        // DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        System.out.println("Begin EditDistance Store");
        DateTimeFormatter formatter = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd HH:mm:ss")
            .optionalStart()
            .appendFraction(ChronoField.MICRO_OF_SECOND, 1, 6, true)
            .optionalEnd()
            .toFormatter();
        ZoneOffset zoneOffset = ZoneOffset.UTC;
        System.out.println("Begin EditDistance Store");
        for (int attr = 0; attr < numAttributes; attr++) {
            for (int i = 0; i < r.size(); i++) {
                for (int j = i + 1; j < r.size(); j++) {
                    String key = i + "," + j + "," + attr;
                    int dist;
                    if (attr == 0) {
                        String time1 = r.get(i).getAttribute(attr).replace("\"", "").trim();
                        String time2 = r.get(j).getAttribute(attr).replace("\"", "").trim();
                        LocalDateTime t1 = LocalDateTime.parse(time1, formatter);
                        LocalDateTime t2 = LocalDateTime.parse(time2, formatter);
                        long millis1 = t1.toInstant(zoneOffset).toEpochMilli();
                        long millis2 = t2.toInstant(zoneOffset).toEpochMilli();
                        dist = (int) Math.abs(millis1 - millis2);
                    } else {
                        dist = computeDistance(
                                r.get(i).getAttribute(attr),
                                r.get(j).getAttribute(attr)
                        );
                    }
                    tempDistances.put(key, dist);
                    maxDistances[attr] = Math.max(maxDistances[attr], dist);
                }
            }
        }
        for (int i = 0; i < r.size(); i++) {
            for (int j = i + 1; j < r.size(); j++) {
                String pairKey = i + "," + j;
                int[] distances = new int[numAttributes];

                for (int attr = 0; attr < numAttributes; attr++) {
                    String key = i + "," + j + "," + attr;
                    int rawDist = tempDistances.get(key);
                    distances[attr] = normalizeDistance(rawDist, maxDistances[attr], min, max);
                }
                distanceCache.put(pairKey, distances);
            }
        }

        List<List<int[]>> distanceValues = calculateMatchingKeys(min,max, ratios);
        Map<Integer, Candidate> candidateMap = new HashMap<>();
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < r.size(); i++) {
            for (int j = i + 1; j < r.size(); j++) {
                String pairKey = i + "," + j;
                int[] editDistances = distanceCache.get(pairKey);

                for (int kk = 0; kk < distanceValues.size(); kk++) {
                    List<int[]> restrictionCombination = distanceValues.get(kk);
                    boolean matches = true;
                    for (int attrIndex = 0; attrIndex < editDistances.length; attrIndex++) {
                        int[] range = restrictionCombination.get(attrIndex);
                        int distance = editDistances[attrIndex];
                        if (distance < range[0] || distance > range[1]) {
                            matches = false;
                            break;
                        }
                    }

                    if (matches) {
                        Candidate matchedCandidate = candidateMap.get(kk);
                        if (matchedCandidate != null) {
                            matchedCandidate.addAgree(pairKey);
                        } else {
                            matchedCandidate = new Candidate(restrictionCombination, totalPairs);
                            matchedCandidate.addAgree(pairKey);
                            candidateMap.put(kk, matchedCandidate);
                        }
                        if (correctPairs.contains(pairKey)) {
                            matchedCandidate.addTruth();
                        }
                    }
                }
            }
        }

        Set<Candidate> potentialKeys = new HashSet<>();
        for (Candidate key : candidateMap.values()) {
            if (key.getConfidence() >= eta_c) {

                potentialKeys.add(key);
            }
        }

        long endTime = System.currentTimeMillis();
        long Time = endTime - startTime;
        System.out.println("CSP Time:"+Time);
        System.out.println("Candidate Num:"+potentialKeys.size());
        return potentialKeys;
    }

    public static class Candidate {
        private List<int[]> restrictionCombination;
        private List<String> agreeSet;
        private double support;
        private int truth;
        private double confidence;
        private int totalPairs;

        public Candidate(List<int[]> restrictionCombination, int totalPairs) {
            this.restrictionCombination = restrictionCombination;
            this.agreeSet = new ArrayList<>();
            this.totalPairs = totalPairs;
            this.truth = 0;
        }

        public void addAgree(String pairKey) {
            agreeSet.add(pairKey);
        }

        public void addTruth(){
            truth = truth + 1;
        }

        public double getConfidence() {
            this.confidence = (double)truth /(double) agreeSet.size();
            return confidence;
        }

        public double getSupport() {
            support = (double) agreeSet.size() / (double)totalPairs;
            return support;
        }

        public int getTotalRange() {
            return restrictionCombination.stream()
                    .mapToInt(range -> range[1] - range[0])
                    .sum();
        }

        public int getTotalPairs() {
            return totalPairs;
        }

        public List<int[]> getDistanceRestrictions() {
            return restrictionCombination;
        }

        public List<String> getAgreeSet() {
            return agreeSet;
        }
        
        public void deleteAgree(String pairKey) {
            agreeSet.remove(pairKey);
        }

        public Candidate copy() {
            Candidate candidateCopy = new Candidate(new ArrayList<>(this.restrictionCombination), this.totalPairs);

            for (String pair : this.agreeSet) {
                candidateCopy.addAgree(pair);
            }

            candidateCopy.truth = this.truth;
            candidateCopy.confidence = this.confidence;
            candidateCopy.support = this.support;
            
            return candidateCopy;
        }
    

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Restriction Combination: ");
            for (int[] restriction : restrictionCombination) {
                sb.append("[");
                sb.append(restriction[0]);
                sb.append(", ");
                sb.append(restriction[1]);
                sb.append("] ");
            }
            sb.append("\n");
            sb.append("Support: ");
            sb.append(support);
            sb.append("\n");
            sb.append("Confidence: ");
            sb.append(confidence);
            sb.append("\n");

            return sb.toString();
        }
    }
    public List<List<int[]>> calculateMatchingKeys(int min, int max, List<Double> ratios) {
        List<List<int[]>> allAttrRanges = calculateDistanceValues(min, max, ratios);

        List<List<int[]>> cartesianProduct = new ArrayList<>();
        cartesianProduct.add(new ArrayList<>());
        
        for (List<int[]> attrRanges : allAttrRanges) {
            List<List<int[]>> newProduct = new ArrayList<>();
            

            for (List<int[]> existingCombination : cartesianProduct) {

                for (int[] range : attrRanges) {
                    List<int[]> newCombination = new ArrayList<>(existingCombination);
                    newCombination.add(range);
                    newProduct.add(newCombination);
                }
            }
            cartesianProduct = newProduct;
        
        return cartesianProduct;
    }
    

    public List<List<int[]>> calculateDistanceValues(int min, int max, List<Double> ratios) {
        List<List<int[]>> allAttrRanges = new ArrayList<>();
        int numAttributes = distanceCache.values().iterator().next().length;
    
        for (int attr = 0; attr < numAttributes; attr++) {
            Map<Integer, Integer> frequencyMap = new HashMap<>();

            for (int[] distances : distanceCache.values()) {
                int dist = distances[attr];
                frequencyMap.put(dist, frequencyMap.getOrDefault(dist, 0) + 1);
            }

            List<Integer> sortedByFreq = new ArrayList<>(frequencyMap.keySet());
            sortedByFreq.sort((a, b) -> frequencyMap.get(b) - frequencyMap.get(a));
            System.out.println(sortedByFreq.size());
            int keepCount = (int) Math.ceil(sortedByFreq.size() * ratios.get(attr));
            Set<Integer> selectedDistances = new TreeSet<>(sortedByFreq.subList(0, Math.min(keepCount, sortedByFreq.size())));

            int minValue = Collections.min(frequencyMap.keySet());
            int maxValue = Collections.max(frequencyMap.keySet());
            selectedDistances.add(minValue);
            selectedDistances.add(maxValue);
            List<Integer> distList = new ArrayList<>(selectedDistances);
            Collections.sort(distList);
            System.out.println(distList);
            List<int[]> attrRanges = new ArrayList<>();
            for (int i = 0; i < distList.size(); i++) {
                for (int j = i; j < distList.size(); j++) {
                    attrRanges.add(new int[]{distList.get(i), distList.get(j)});
                }
            }
    
            allAttrRanges.add(attrRanges);
        }
    
        return allAttrRanges;
    }
    

    private int normalizeDistance(double rawDistance, double maxDistance, int min, int max) {
        if (maxDistance == 0) return 1;
        double normalized = Math.log(1 + rawDistance) / Math.log(1 + maxDistance);
        return min + (int) Math.floor(normalized * (max -1));
    }

    public int computeDistance(String s1, String s2) {
        int len1 = s1.length();
        int len2 = s2.length();
        int[][] dp = new int[len1 + 1][len2 + 1];

        for (int i = 0; i <= len1; i++) dp[i][0] = i;
        for (int j = 0; j <= len2; j++) dp[0][j] = j;

        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = Math.min(dp[i - 1][j - 1],
                               Math.min(dp[i - 1][j],
                                        dp[i][j - 1]))
                             + 1;
                }
            }
        }
        return dp[len1][len2];
    }
    
    public static class Tuple {
        private List<String> attributes;

        public Tuple(String... attributes) {
            this.attributes = Arrays.asList(attributes);
        }

        public String getAttribute(int index) {
            return attributes.get(index);
        }

        public int size() {
            return attributes.size();
        }

        @Override
        public String toString() {
            return String.join(", ", attributes);
        }
    }

}
