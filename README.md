# MatchingKey

## 📁 Project Description

### 1. Method Implementations (Located in `keys/src/`)

This directory contains the implementation code for candidate key generation and final matching key set selection algorithms:

#### 🔹 Candidate Key Generation

- `CandidateKey.java`  
  Implements the **CS** method.

- `CandidateKeyGeneration.java`  
  Implements the **CSP** method.

- `CandidateKeyNegative.java`  
  Implements the **MCG** method.

#### 🔹 Matching Key Set Selection

- `GreedyAlgorithm.java`  
  Implements the **GA (Greedy Algorithm)** method.

- `GreedyAlgorithmWithPruning.java`  
  Implements the **GAP (Greedy Algorithm with Pruning)** method.

- `MCG_GA.java`  
  Implements the **combination of MCG and GA** methods.

- `MCG_GAP.java`  
  Implements the **combination of MCG and GAP** methods.

---

### 2. Dataset Files (Located in `data/`)

- `fz.arff`  
  Represents the **Restaurant dataset**.

- Other `.arff` files  
  Correspond to datasets with matching names and are used for algorithm testing and evaluation.

---

## 📌 Usage Notes

Before running the project, make sure the main class entry point is correctly set in the `src` directory and that the corresponding `.arff` dataset files from the `data` folder are loaded properly.
