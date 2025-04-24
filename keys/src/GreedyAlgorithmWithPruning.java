package src;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

import src.CandidateKey.Candidate;
import src.CandidateKey.Tuple;

public class GreedyAlgorithmWithPruning {

    public static Set<Candidate> greedyAlgorithmWithPruning(Set<Candidate> psi, double eta_s, int min, int max) {
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
                int attrIndex = 0;
                for (int[] range : candidate.getDistanceRestrictions()){
                    int[] range_p1 = psi_p1.getDistanceRestrictions().get(attrIndex);
                    int[] range_p5 = psi_p5.getDistanceRestrictions().get(attrIndex);
                    int[] range_p6 = psi_p6.getDistanceRestrictions().get(attrIndex);

                    if ((range_p1 != null && range[0] <= range_p1[0] && range[1] >= range_p1[1]) ||
                            (range_p5 != null && range_p5[0] <= range[0] && range_p5[1] >= range[1]) ||
                            (range_p6 != null && range_p6[0] <= range[0] && range_p6[1] >= range[1])) {
                        shouldUpdate = true;
                        break;
                    }
                    attrIndex = attrIndex + 1;
                }

                if (shouldUpdate) {
                    List<String> bestCandidateAgreeSet = new ArrayList<>(bestCandidateCopy.getAgreeSet());
                    List<String> candidateAgreeSet = new ArrayList<>(candidate.getAgreeSet());
                    for (String candidatePair : candidateAgreeSet) {
                        for (String bestCandidatePair : bestCandidateAgreeSet) {
                            if (candidatePair.equals(bestCandidatePair)) {
                                candidate.deleteAgree(candidatePair);
                                break;
                            }
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

    private static Candidate calculatePsiP1(List<int[]> originalRestrictions, int totalPairs, int min,
            int max) {
        List<int[]> newRestrictions = new ArrayList<>();
        for (int[] range : originalRestrictions) {
            if (range[0] == min || range[1] == max) {
                newRestrictions.add(new int[] {-1, 11});
            } else {
                int[] newRange = new int[] { range[0] - 1, range[1] + 1 };
                newRestrictions.add(newRange);
            }
        }
        return new Candidate(newRestrictions, totalPairs);
    }

    private static Candidate calculatePsiP5(List<int[]> originalRestrictions, int totalPairs, int min) {
        List<int[]> newRestrictions = new ArrayList<>();

        for (int[] range : originalRestrictions) {
            if (range[0] == min) {
                newRestrictions.add(new int[] {-1, -1});
            } else {
                int[] newRange = new int[] { min, range[0] - 1 };
                newRestrictions.add(newRange);
            }
        }
        return new Candidate(newRestrictions, totalPairs);
    }

    private static Candidate calculatePsiP6(List<int[]> originalRestrictions, int totalPairs, int max) {
        List<int[]> newRestrictions = new ArrayList<>();
        for (int[] range : originalRestrictions) {
            if (range[1] == max) {
                newRestrictions.add(new int[] {11, 11});
            } else {
                int[] newRange = new int[] { range[1] + 1, max };
                newRestrictions.add(newRange);
            }
        }
        return new Candidate(newRestrictions, totalPairs);
    }


    public static void main(String[] args) {
        String filePath = "../data/retail.arff";
        double[] c = {140.0,150.0};
        for (int mm = 0; mm <=1; mm++){
            for (int m = 100; m <= 1300; m += 200) {
                System.out.println("Processing with m = " + m);
                List<Tuple> r = new ArrayList<>();
                Set<String> correctPairs = new HashSet<>();
                int[] selectedColumns = { 0, 1,2,3};

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
                    for (int i = 0; i < Math.min(m, data.size()); i += 1) {
                        for (int j = i + 1; j < Math.min(m, data.size()); j++) {
                            if (data.get(i).length > 4 && data.get(j).length > 4 ){
                                if (data.get(i)[4].equals(data.get(j)[4])) {
                                    String pairKey = i + "," + j;
                                    correctPairs.add(pairKey);
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                double eta_c = 1.0;
                int min = 1;
                int max = 10;
                List<Double> ratios = Arrays.asList(1.0, 1.0, 1.0,1.0); 
                CandidateKey CS = new CandidateKey();
                Set<Candidate> candidateSet = CS.preprocessAndCalculateDistances(r, eta_c, correctPairs, min, max,ratios);
                List<Candidate> candidateList = new ArrayList<>(candidateSet);

                candidateList.sort((c1, c2) -> 
                    Integer.compare(c2.getTotalRange(), c1.getTotalRange())
                );
            
                candidateSet = new LinkedHashSet<>(candidateList);

                double eta_s = c[mm] / 844350;
                long startTimeMillis = System.currentTimeMillis();
                Set<Candidate> resultSet = greedyAlgorithmWithPruning(candidateSet, eta_s, min,
                        max);
                long endTimeMillis = System.currentTimeMillis();
                long executionTimeMillis = endTimeMillis - startTimeMillis;
                System.out.println("GAP Execution time (milliseconds): " + executionTimeMillis);
                System.out.println("Matching Key Set size:" + resultSet.size());
                for (Candidate candidate : resultSet) {
                    System.out.println(candidate.toString());
                }
            }
        }
    }
}
