# MatchingKey

## 📁 Project Description

### 1. Method Implementations (Located in `keys/src/`)

This directory contains the implementation code for candidate generation and final matching key set selection algorithms:

#### 🔹 Candidate Generation

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
  Correspond to datasets with matching names and are used for algorithm training and evaluation.

---

## 🚀 How to Run

You can execute different methods by running the corresponding main class file in the `keys/src/` directory:

|**Method Combination**|       **Main File to Run**        |      
|  ------------------  | --------------------------------- | 
|  **CS/CSP + GA**     | `GreedyAlgorithm.java`            | 
|  **CS/CSP + GAP**    | `GreedyAlgorithmWithPruning.java` | 
|  **MCG + GA**        | `MCG_GA.java`                     | 
|  **MCG + GAP**       | `MCG_GAP.java`                    | 
