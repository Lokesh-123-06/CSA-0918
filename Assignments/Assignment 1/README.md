# Assignment 1 – Campus Smart-Parking System

**Name:** Lokesh  
**Register Number:** To be filled in by the student  
**Course:** Programming in Java – CO1  
**Java version:** Java 17 or later

This folder contains solutions for all 12 tasks, completed Java programs, design explanations, complexity comparisons and test evidence.

## Structure
- `Task03/ParkingFacility.java` – completed supplied allocation/release program.
- `Task08/FeeDemo.java` – completed inheritance, overriding, polymorphism and interface program.
- `src/SmartParkingSystem.java` – integrated implementation for the full smart-parking model and Tasks 11–12 tests.
- `REPORT.md` – answers for Tasks 1–12, pseudocode, UML, alternatives and evaluation.
- `OUTPUTS.md` – expected and observed console outputs.

## Compile
From this folder:

```bash
javac -d out Task03/ParkingFacility.java Task08/FeeDemo.java src/SmartParkingSystem.java
```

## Run
```bash
java -cp out ParkingFacility
java -cp out FeeDemo
java -cp out SmartParkingSystem
```

The third program runs automated checks for allocation, duplicate registration, hourly boundaries, zero duration, EV charging, passes, full capacity, invalid slots and the new e-bike/delivery-van categories.

## Submission note
Run the programs on your own computer and add the required execution screenshots to this folder before final academic submission. The console outputs are recorded in `OUTPUTS.md`.
