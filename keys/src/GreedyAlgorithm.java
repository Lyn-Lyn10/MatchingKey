package src;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

import src.CandidateKeyGeneration.Candidate;
import src.CandidateKeyGeneration.Tuple;

public class GreedyAlgorithm {
    public static Set<Candidate> greedyAlgorithm(Set<Candidate> candidateSet, double eta_s, int min, int max) {
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
        double[] nc = {140.0,150.0};
        for (int num = 0; num <=1; num++){
            for (int m = 100; m <= 1300; m += 200) {
                List<Tuple> r = new ArrayList<>();
                Set<String> correctPairs = new HashSet<>();
                int[] selectedColumns = { 0, 1, 2, 3};
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
                CandidateKeyGeneration CS = new CandidateKeyGeneration();
                Set<Candidate> candidateSet = CS.preprocessAndCalculateDistances(r, eta_c, correctPairs, min, max,ratios);
                List<Candidate> candidateList = new ArrayList<>(candidateSet);
                candidateList.sort((c1, c2) -> 
                    Integer.compare(c2.getTotalRange(), c1.getTotalRange())
                );
            
                candidateSet = new LinkedHashSet<>(candidateList);

                double eta_s = nc[num] / 844350;
                long startTimeMillis = System.currentTimeMillis();
                Set<Candidate> resultSet =  greedyAlgorithm(candidateSet, eta_s, min,
                        max);
                long endTimeMillis = System.currentTimeMillis();
                long executionTimeMillis = endTimeMillis - startTimeMillis;
                System.out.println("GA Execution time (milliseconds): " + executionTimeMillis);
                System.out.println("Matching Key Set size:" + resultSet.size());
                for (Candidate candidate : resultSet) {
                    System.out.println(candidate.toString());
                }
            }
        }
    }
}
