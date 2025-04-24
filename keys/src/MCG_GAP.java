package src;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import src.CandidateKeyNegative.Candidate;
import src.CandidateKeyNegative.EditDistanceUtil;
import src.CandidateKeyNegative.Tuple;


public class MCG_GAP {
    static Map<String, int[]> distanceCache = new HashMap<>();
    public static Set<Candidate> GAPMCG(Set<Candidate> psi, double eta_s, int[] min, int[] max) {
        Set<Candidate> Ψo = new HashSet<>();
        double supp_s_psi_o = 0;
        while (!psi.isEmpty()) {
            if (supp_s_psi_o >= eta_s) {
                break;
            }
            Candidate bestCandidate = null;
            double maxSupport = -1;

            for (Candidate candidate : psi) {
                double currentSupport = candidate.getSupport();
                if (currentSupport > maxSupport) {
                    maxSupport = currentSupport;
                    bestCandidate = candidate;
                }
            }

            if (maxSupport == 0) {
                break;
            }
            Candidate bestCandidateCopy = bestCandidate.copy();
            Ψo.add(bestCandidateCopy);
            supp_s_psi_o += bestCandidateCopy.getSupport();
            psi.remove(bestCandidate);
            System.out.println(supp_s_psi_o);
            Candidate psi_p1 = calculatePsiP1(bestCandidateCopy.getDistanceRestrictions(),
                    bestCandidateCopy.getTotalPairs(), min, max);
            Candidate psi_p5 = calculatePsiP5(bestCandidateCopy.getDistanceRestrictions(),
                    bestCandidateCopy.getTotalPairs(), min);
            Candidate psi_p6 = calculatePsiP6(bestCandidateCopy.getDistanceRestrictions(),
                    bestCandidateCopy.getTotalPairs(), max);
            Iterator<Candidate> iterator = psi.iterator();
            while (iterator.hasNext()) {
                Candidate candidate = iterator.next();
                boolean shouldUpdate = false;

                List<int[]> distanceRestrictions = candidate.getDistanceRestrictions();
                for (int attrIndex = 0; attrIndex < distanceRestrictions.size(); attrIndex++) {
                    int[] range = distanceRestrictions.get(attrIndex);
                    int[] range_p1 = psi_p1.getDistanceRestrictions().get(attrIndex);
                    int[] range_p5 = psi_p5.getDistanceRestrictions().get(attrIndex);
                    int[] range_p6 = psi_p6.getDistanceRestrictions().get(attrIndex);

                    if ((range_p1 != null && range[0] <= range_p1[0] && range[1] >= range_p1[1]) ||
                            (range_p5 != null && range_p5[0] <= range[0] && range_p5[1] >= range[1]) ||
                            (range_p6 != null && range_p6[0] <= range[0] && range_p6[1] >= range[1])) {
                        shouldUpdate = true;
                        break;
                    }
                }

                if (shouldUpdate) {
                    Set<String> bestCandidateAgreeSet = bestCandidateCopy.getAgreeSet();
                    Set<String> candidateAgreeSet = new HashSet<>(candidate.getAgreeSet());
                    for (String candidatePairKey : candidateAgreeSet) {
                        if (bestCandidateAgreeSet.contains(candidatePairKey)) {
                            candidate.deleteAgree(candidatePairKey);
                        }
                    }
                } else {
                    iterator.remove();
                }
            }
        }

        if (supp_s_psi_o < eta_s) {
            return Collections.emptySet();
        } else {
            return Ψo;
        }
    }

    private static Candidate calculatePsiP1(List<int[]> originalRestrictions, int totalPairs, int[] min,
            int[] max) {
        List<int[]> newRestrictions = new ArrayList<>();
        int i = 0;
        for (int j = 0; j < originalRestrictions.size(); j++) {
            int[] range = originalRestrictions.get(j);

            if (range[0] == min[i] || range[1] == max[i]) {
                newRestrictions.add(new int[] {-1, 10});
            } else {
                int[] newRange = new int[] { range[0] - 1, range[1] + 1 };
                newRestrictions.add(newRange);
            }
            i = i + 1;
        }

        return new Candidate(newRestrictions, totalPairs);
    }

    private static Candidate calculatePsiP5(List<int[]> originalRestrictions, int totalPairs, int[] min) {
        List<int[]> newRestrictions = new ArrayList<>();
        int i = 0;
        for (int j = 0; j < originalRestrictions.size(); j++) {
            int[] range = originalRestrictions.get(j);

            if (range[0] == min[i]) {
                newRestrictions.add(new int[] {-1, -1});
            } else {
                int[] newRange = new int[] { min[i], range[0] - 1 };
                newRestrictions.add(newRange);
            }
            i = i + 1;
        }

        return new Candidate(newRestrictions, totalPairs);
    }

    private static Candidate calculatePsiP6(List<int[]> originalRestrictions, int totalPairs, int[] max) {
        List<int[]> newRestrictions = new ArrayList<>();
        int i = 0;

        for (int j = 0; j < originalRestrictions.size(); j++) {
            int[] range = originalRestrictions.get(j);

            if (range[1] == max[i]) {
                newRestrictions.add(new int[] {10, 10});
            } else {
                int[] newRange = new int[] { range[1] + 1, max[i] };
                newRestrictions.add(newRange);
            }
            i = i + 1;
        }
        return new Candidate(newRestrictions, totalPairs);
    }

    private static int normalizeDistance(double rawDistance, double maxDistance, int min, int max) {
        if (maxDistance == 0) return 1;
        double normalized = Math.log(1 + rawDistance) / Math.log(1 + maxDistance);
        return min + (int) Math.floor(normalized * (max ));
    }

    public static Map<Integer, List<Integer>> calculateDistanceValues(int min, int max, List<Double> ratios) {
        Map<Integer, List<Integer>> allAttrRanges = new HashMap<>();
        int numAttributes = distanceCache.values().iterator().next().length;
    
        for (int attr = 0; attr < numAttributes; attr++) {
            Map<Integer, Integer> frequencyMap = new HashMap<>();

            for (int[] distances : distanceCache.values()) {
                int dist = distances[attr];
                frequencyMap.put(dist, frequencyMap.getOrDefault(dist, 0) + 1);
            }

            List<Integer> sortedByFreq = new ArrayList<>(frequencyMap.keySet());
            sortedByFreq.sort((a, b) -> frequencyMap.get(b) - frequencyMap.get(a));

            int keepCount = (int) Math.ceil(sortedByFreq.size() * ratios.get(attr));
            Set<Integer> selectedDistances = new TreeSet<>(sortedByFreq.subList(0, Math.min(keepCount, sortedByFreq.size())));

            Set<Integer> actualValues = new HashSet<>();
            for (int[] distances : distanceCache.values()) {
                actualValues.add(distances[attr]);
            }
            if (!actualValues.isEmpty()) {
                int minValue = Collections.min(actualValues);
                int maxValue = Collections.max(actualValues);
                selectedDistances.add(minValue);
                selectedDistances.add(maxValue);
            }

            List<Integer> distList = new ArrayList<>(selectedDistances);
            Collections.sort(distList);
            allAttrRanges.put(attr, distList);
        }
    
        return allAttrRanges;
    }
    

    public static void main(String[] args) {
        String filePath = "../data/retail.arff";
        double[] c = {150.0,140.0};
        for (int mm = 0; mm <=1; mm++){
            System.out.println(c[mm]);
            for (int m = 100; m <= 1300; m += 200) {

                List<Tuple> r = new ArrayList<>();
                Set<String> pPlusPsi0 = new HashSet<>();
                Set<String> qMinusPsi0 = new HashSet<>();
                int[] selectedColumns = { 0,1, 2,3};
                int maxn = 4;
                try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
                    String line;
                    boolean dataSection = false;
                    List<String[]> data = new ArrayList<>();
                    Pattern csvPattern = Pattern.compile(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

                    while ((line = br.readLine()) != null) {
                        line = line.trim();
                        if (line.equalsIgnoreCase("@data")) {
                            dataSection = true;
                            continue;
                        }

                        if (dataSection && !line.isEmpty()) {
                            String[] values = csvPattern.split(line);
                            for (int i = 0; i < values.length; i++) {
                                values[i] = values[i].replaceAll("\"", "").replaceAll("^'|'$", "").trim();
                            }
                            data.add(values);
                        }
                    }

                    for (int i = 0; i < Math.min(m, data.size()); i++) {
                        String[] values = data.get(i);
                        String[] selectedValues = new String[selectedColumns.length];
                        for (int j = 0; j < selectedColumns.length; j++) {
                            selectedValues[j] = values[selectedColumns[j]];
                        }
                        r.add(new Tuple(selectedValues));
                    }

                    for (int i = 0; i < Math.min(m, data.size()); i++) {
                        for (int j = i + 1; j < Math.min(m, data.size()); j++) {
                            if (data.get(i).length > maxn && data.get(j).length > maxn) {
                                String pairKey = i + "," + j;
                                if (data.get(i)[maxn].equals(data.get(j)[maxn])) {
                                    pPlusPsi0.add(pairKey);
                                } else {
                                    qMinusPsi0.add(pairKey);
                                }
                            }
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }

                System.out.println(r.size());
                System.out.println(pPlusPsi0.size());
                System.out.println(qMinusPsi0.size());
                double eta_c = 0.8;
                System.out.println("eta_c:" + eta_c);
                int s = r.get(0).size();
                List<Double> percentages = Arrays.asList(1.0, 1.0, 1.0, 1.0);
                int min =0;
                int max =9;
                double[] maxDistances = new double[s];
                Map<String, Integer> distanceCount = new HashMap<>();
                // DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                    .appendPattern("yyyy-MM-dd HH:mm:ss")
                    .optionalStart()
                    .appendFraction(ChronoField.MICRO_OF_SECOND, 1, 6, true)
                    .optionalEnd()
                    .toFormatter();

                ZoneOffset zoneOffset = ZoneOffset.UTC;
                for (int attr = 0; attr < s; attr++) {
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
                                dist = EditDistanceUtil.calculateEditDistance(
                                        r.get(i).getAttribute(attr),
                                        r.get(j).getAttribute(attr)
                                );
                            }
                            distanceCount.put(key, dist);
                            maxDistances[attr] = Math.max(maxDistances[attr], dist);
                        }
                    }
                }

                for (int i = 0; i < r.size(); i++) {
                    for (int j = i + 1; j < r.size(); j++) {
                        String pairKey = i + "," + j;
                        int[] distances = new int[s];
        
                        for (int attr = 0; attr < s; attr++) {
                            String key = i + "," + j + "," + attr;
                            int rawDist = distanceCount.get(key);
                            distances[attr] = normalizeDistance(rawDist, maxDistances[attr], min, max);
                        }
                        distanceCache.put(pairKey, distances);
                    }
                }
                Map<Integer, List<Integer>> topdistanceMap = calculateDistanceValues(min, max,percentages);
                List<int[]> distanceRestrictions = new ArrayList<>();

                for (Map.Entry<Integer, List<Integer>> entry : topdistanceMap.entrySet()) {
                    List<Integer> values = entry.getValue();
                    System.out.println(values);
                    if (!values.isEmpty()) {
                        int minVal = Collections.min(values);
                        int maxVal = Collections.max(values);
                        distanceRestrictions.add(new int[]{minVal, maxVal});
                    }
                }
                int totalPairs = r.size() * (r.size() - 1) / 2;
                Candidate psi = new Candidate(distanceRestrictions, totalPairs);
                CandidateKeyNegative generator = new CandidateKeyNegative();
                for (String pair : pPlusPsi0) {
                    psi.addpositive(pair);
                }

                System.out.println("Begin MCG");
                long startTime = System.currentTimeMillis();
                Set<Candidate> candidateSet = generator.MCG(psi, pPlusPsi0, qMinusPsi0, eta_c, topdistanceMap, distanceCache);
                long endTime = System.currentTimeMillis();
                long executionTime = endTime - startTime;
                System.out.println("MCG time (milliseconds): " + executionTime);
                System.out.println("candidate set size:" + candidateSet.size());
                for (Candidate candidate : candidateSet) {
                    // System.out.println(candidate.toString());
                }
                generator.printCallCounts();
                System.out.println("gd num:" + pPlusPsi0.size());
                double eta_s = c[mm] / 844350;
                long startTimeMillis = System.currentTimeMillis();
                int[] minn = {0,0,0,0};
                int[] maxx = {9,9,9,9};
                Set<Candidate> resultSet = GAPMCG(candidateSet, eta_s, minn, maxx);

                long endTimeMillis = System.currentTimeMillis();
                long executionTimeMillis = endTimeMillis - startTimeMillis;
                System.out.println("GAP time (milliseconds): " + executionTimeMillis);
                System.out.println("Set size:" + resultSet.size());
                System.out.println("Resulting Candidate Set:");
                for (Candidate candidate : resultSet) {
                    System.out.println(candidate.toString());
                }
                }
        }
    }
}
