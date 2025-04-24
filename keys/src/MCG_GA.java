package src;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import src.CandidateKeyNegative.Candidate;
import src.CandidateKeyNegative.EditDistanceUtil;
import src.CandidateKeyNegative.Tuple;

public class MCG_GA {
    static Map<String, int[]> distanceCache = new HashMap<>();
    public static Set<Candidate> MCGGA(Set<Candidate> candidateSet, double eta_s) {
        Set<Candidate> Ψo = new HashSet<>();
        double supp_s = 0;
        while (!candidateSet.isEmpty()) {
            Candidate bestCandidate = null;
            double maxSupport = -1;

            for (Candidate candidate : candidateSet) {
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
            supp_s += bestCandidateCopy.getSupport();
            candidateSet.remove(bestCandidate);
            List<String> bestCandidateAgreeSet = new ArrayList<>(bestCandidateCopy.getAgreeSet());
            Iterator<Candidate> iteratorr = candidateSet.iterator();
            while (iteratorr.hasNext()) {
                Candidate candidate = iteratorr.next();
                List<String> candidateAgreeSet = new ArrayList<>(candidate.getAgreeSet());
                for (String candidatePair : candidateAgreeSet) {
                    for (String bestCandidatePair : bestCandidateAgreeSet) {
                        if (candidatePair.equals(bestCandidatePair)) {
                            candidate.deleteAgree(candidatePair);
                            break;
                        }
                    }
                }
            }
            if (supp_s >= eta_s) {
                break;
            }

        }
        if (supp_s < eta_s) {
            return new HashSet<>();
        }

        return Ψo;
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
                Set<Candidate> resultSet = MCGGA(candidateSet, eta_s, minn, maxx);

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
