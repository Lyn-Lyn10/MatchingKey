package src;

import java.util.*;

public class CandidateKeyNegative {
    private final Set<String> distanceCache = new HashSet<>();

    public Set<Candidate> MS(Candidate psi, Set<String> qMinusPsi0, Set<String> pPlusPsi0, Map<Integer, List<Integer>> topDistancesMap,
    Map<String, int[]> mapHash) {

        Set<Candidate> PsiS = new HashSet<>();
        for (int attrIndex = 0; attrIndex < psi.getDistanceRestrictions().size(); attrIndex++) {
            int[] cPsi = psi.getDistanceRestrictions().get(attrIndex);
            int dv = cPsi[0];
            int du = cPsi[1];
            if (dv == du)
                continue;
            List<Integer> topDistances = topDistancesMap.get(attrIndex);
            int dSMax = dv;
            int dSMin = du;
            if((du - dv) != 1){
                for (int d : topDistances) {
                    if (d > dv) {
                        dSMin = d;
                        break;
                    }
                }
                for (int i = topDistances.size() - 1; i >= 0; i--) {
                    int d = topDistances.get(i);
                    if (d < du) {
                        dSMax = d;
                        break;
                    }
                }
            }
            if (dSMax != du) {
                List<int[]> copiedRestrictions = new ArrayList<>();
                for (int[] range : psi.getDistanceRestrictions()) {
                    copiedRestrictions.add(Arrays.copyOf(range, range.length));
                }
                Candidate psi1 = new Candidate(copiedRestrictions, psi.getTotalPairs());

                psi1.getDistanceRestrictions().set(attrIndex, new int[] { dv, dSMax });
                psi1.reSerial();
                PsiS.add(psi1);
            }
            if (dSMin != dv) {

                List<int[]> copiedRestrictions = new ArrayList<>();
                for (int[] range : psi.getDistanceRestrictions()) {
                    copiedRestrictions.add(Arrays.copyOf(range, range.length));
                }
                Candidate psi2 = new Candidate(copiedRestrictions, psi.getTotalPairs());

                psi2.getDistanceRestrictions().set(attrIndex, new int[] { dSMin, du });
                psi2.reSerial();
                PsiS.add(psi2);
            }
        }
        Iterator<Candidate> iterator = PsiS.iterator();
        while (iterator.hasNext()) {
            Candidate psiS = iterator.next();
            String key = psiS.getSerial();
            if (distanceCache.contains(key)) {
                iterator.remove(); 
                continue;
            }
            for (String pair : new HashSet<>(psi.getPositiveSet())) {
                if (psiS.matches(pair, mapHash)) {
                    psiS.addpositive(pair);
                }
            }
            if (psiS.getPositiveSetSize() <= 0) {
                distanceCache.add(key);
                iterator.remove(); 
            }
        }
        return PsiS;
    }


    // Algorithm 5: Candidate Generation with Negative Pruning MCG(𝜓, 𝑝+𝜓0,𝑞−𝜓0)
    public Set<Candidate> MCG(Candidate psi, Set<String> pPlusPsi0, Set<String> qMinusPsi0, Double eta_c,
            Map<Integer, List<Integer>> topDistancesMap, Map<String, int[]> mapHash) {
        Set<Candidate> Psi0 = new HashSet<>();
        Psi0.add(psi);
        boolean label = false;
        Set<String> p = new HashSet<>(qMinusPsi0);
        for (String pair : qMinusPsi0) {
            Set<Candidate> PsiA = new HashSet<>();
            Set<Candidate> PsiR = new HashSet<>();
            if (psi.matches(pair, mapHash)) {
                psi.addNegative(pair);
                double confidence = (double) psi.getPositiveSetSize() / (psi.getAgreeSet().size());
                if (Double.isNaN(confidence) || confidence < eta_c || confidence == 0.0) {
                    PsiR.add(psi);
                    Set<Candidate> PsiS = MS(psi, p, psi.getPositiveSet(), topDistancesMap,mapHash);
                    for (Candidate psiS : PsiS) {
                        String key = psiS.getSerial();
                        if (!distanceCache.contains(key)) {
                            distanceCache.add(key);
                            PsiA.addAll(MCG(psiS, psiS.getPositiveSet(), p, eta_c, topDistancesMap, mapHash));
                        }
                    }
                    label = true;
                }
            } else {
                p.remove(pair);
            }
            for (Candidate psii : PsiA) {
                if (psii.getSupport() == 0) {
                    PsiR.add(psii);
                }
            }
            Psi0.addAll(PsiA);
            Psi0.removeAll(PsiR);
            if (label == true)
                break;
        }
        for (Candidate psii : Psi0) {
            List<int[]> distanceRestrictions = psii.getDistanceRestrictions();
        
            for (int attrIndex = 0; attrIndex < distanceRestrictions.size(); attrIndex++) {
                int[] restrictions = distanceRestrictions.get(attrIndex);
                List<int[]> subsets = generateSubsets(restrictions, topDistancesMap.get(attrIndex));
        
                for (int[] subset : subsets) {
                    List<int[]> newRestrictions = new ArrayList<>();
                    for (int i = 0; i < distanceRestrictions.size(); i++) {
                        if (i == attrIndex) {
                            newRestrictions.add(subset);
                        } else {
                            newRestrictions.add(distanceRestrictions.get(i));
                        }
                    }
                    String key = serializeRestrictions(newRestrictions);
        
                    if (!distanceCache.contains(key)) {
                        distanceCache.add(key);
                    }
                }
            }
            
        }  
        return Psi0;
    }

    private String serializeRestrictions(List<int[]> restrictions) {
        StringBuilder sb = new StringBuilder();
        for (int[] r : restrictions) {
            sb.append(r[0]).append(r[1]);
        }
        return sb.toString();
    }

    public List<int[]> generateSubsets(int[] range, List<Integer> topDistances) {
        List<int[]> subsets = new ArrayList<>();

        for (int i = 0; i <= (topDistances.size() - 1); i++) {
            for (int j = i; j < topDistances.size(); j++) {
                if (topDistances.get(i) >= range[0] && topDistances.get(j) <= range[1]) {
                    int[] subset = { topDistances.get(i), topDistances.get(j) };
                    subsets.add(subset);
                } else if (topDistances.get(i) < range[0] || topDistances.get(i) > range[1]) {
                    break;
                }
            }
        }
        return subsets;
    }

    public static class EditDistanceUtil {
        public static int calculateEditDistance(String val1, String val2) {
            int m = val1.length();
            int n = val2.length();
            int[][] dp = new int[m + 1][n + 1];

            for (int i = 0; i <= m; i++) {
                for (int j = 0; j <= n; j++) {
                    if (i == 0) {
                        dp[i][j] = j;
                    } else if (j == 0) {
                        dp[i][j] = i;
                    } else {
                        int cost = (val1.charAt(i - 1) == val2.charAt(j - 1)) ? 0 : 1;
                        dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
                    }
                }
            }
            return dp[m][n];
        }
    }

    public static class Candidate {
        private List<int[]> distanceRestrictions;
        private Set<String> agreeSet;
        private Set<String> positiveSet;
        private Set<String> negativeSet; 
        private double support;
        private double confidence;
        private int totalPairs;
        private int truth;
        private String serial;

        public Candidate(List<int[]> distanceRestrictions, int totalPairs) {
            this.distanceRestrictions = distanceRestrictions;
            this.positiveSet = new HashSet<>();
            this.agreeSet = new HashSet<>();
            this.negativeSet = new HashSet<>();
            this.totalPairs = totalPairs;
            this.truth = 0;
            StringBuilder sb = new StringBuilder();
            for (int[] r : distanceRestrictions) {
                sb.append(r[0]).append(r[1]);
            }
            this.serial = sb.toString(); 
        }
        public void reSerial(){
            StringBuilder sb = new StringBuilder();
            for (int[] r : distanceRestrictions) {
                sb.append(r[0]).append(r[1]);
            }
            this.serial = sb.toString(); 
        }
        public void addAgree(String pairKey) {
            agreeSet.add(pairKey);
        }
        public String getSerial() {
            return this.serial;
        }
        public Candidate copy() {
            List<int[]> copiedRestrictions = new ArrayList<>();
            for (int[] range : this.distanceRestrictions) {
                copiedRestrictions.add(Arrays.copyOf(range, range.length));
            }
            Candidate candidateCopy = new Candidate(copiedRestrictions, this.totalPairs);
            for (String pair : this.agreeSet) {
                candidateCopy.addAgree(pair);
            }
            for (String pair : this.negativeSet) {
                candidateCopy.addNegative(pair);
            }
            for (String pair : this.positiveSet) {
                candidateCopy.addpositive(pair);
            }
            candidateCopy.truth = this.truth;
            candidateCopy.confidence = this.confidence;
            candidateCopy.support = this.support;

            return candidateCopy;
        }

        public boolean matches(String pair, Map<String, int[]> mapHash) {
            int[] distances = mapHash.get(pair);
            if (distances == null) {
                // System.out.println("null");
                return false;
            }
        
            for (int attrIndex = 0; attrIndex < distanceRestrictions.size(); attrIndex++) {
                int[] restriction = distanceRestrictions.get(attrIndex);
                int distance = distances[attrIndex];
        
                if (distance < restriction[0] || distance > restriction[1]) {
                    return false;
                }
            }
            return true;
        }

        public void addNegative(String pair) {
            negativeSet.add(pair);
            agreeSet.add(pair);
        }

        public void addpositive(String pair) {
            positiveSet.add(pair);
            agreeSet.add(pair);
        }

        public double getSupport() {
            support = (double) agreeSet.size() / totalPairs;
            return support;
        }

        public int getTotalPairs() {
            return totalPairs;
        }

        public List<int[]> getDistanceRestrictions() {
            return distanceRestrictions;
        }

        public Set<String> getAgreeSet() {
            return agreeSet;
        }

        public Set<String> getNegativeSet() { // Getter for negative set
            return negativeSet;
        }

        public Set<String> getPositiveSet() { // Getter for positive set
            return positiveSet;
        }

        public int getPositiveSetSize() {
            return positiveSet.size();
        }

        public int getNegativeSetSize() {
            return negativeSet.size();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Candidate:\n");
            sb.append("Restrictions:\n");

            for (int i = 0; i < distanceRestrictions.size(); i++) {
                int[] range = distanceRestrictions.get(i);
                sb.append(String.format("Attribute %d: [%d, %d]\n", i, range[0], range[1]));
            }
            confidence = (double)positiveSet.size() /(double) agreeSet.size();
            support = (double) agreeSet.size() / totalPairs;
            sb.append(String.format("Support: %.6f\n", support));
            sb.append(String.format("Confidence: %.2f\n", confidence));

            return sb.toString();
        }

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
