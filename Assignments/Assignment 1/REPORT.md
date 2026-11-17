# Assignment 1 – Design, Implement and Validate a Campus Smart-Parking System

**Course:** Programming in Java (CSA09) – CO1  
**Student:** Lokesh  
**Register number:** To be filled in by the student  
**Total:** 50 marks  
**Implementation target:** Java 17+

> The assignment PDF asks for both design reasoning and executable evidence. The answers below follow Tasks 1–12 in the supplied order. The two supplied “Complete the Code” programs are kept in their own folders; the integrated implementation is in `src/SmartParkingSystem.java`.

---

## Task 1 – Understand the Problem and Identify the Objects (4 marks)

### Inputs and outputs
Inputs: vehicle registration number and category; user category (student, staff or visitor); valid pass details; entry/exit timestamps; requested charging energy; floor/slot configuration.  
Outputs: allocated floor and slot, unique ticket, entry time, exit fee, charging fee, occupancy, and useful error messages when no slot is available or an operation is invalid.

### Candidate classes and responsibilities
- **Vehicle:** common registration identity and fee behaviour; subclasses represent two-wheelers, cars and EVs.
- **ParkingSlot:** represents one numbered bay, its charging capability and current occupant.
- **ParkingFacility:** owns floors and slots; allocates and releases slots.
- **ParkingTicket:** records unique ticket number, vehicle, slot and entry time.
- **ParkingService:** coordinates entry, exit, active-ticket lookup, pass validation and payment calculation.
- **ParkingPass:** abstraction for monthly and one-day validity.
- **PricingRule:** encapsulates the pricing policy so a new tariff can be added without changing vehicle identity classes.
- **User/Pass types:** represent student, staff and visitor eligibility. The implementation models eligibility through pass objects rather than hard-coding fee checks in each vehicle.

Nouns suggest model classes; verbs such as allocate, release, enter, exit, validate and calculateFee become methods.

### Business rules and assumptions
1. A slot can contain at most one vehicle.
2. A registration number can have at most one active ticket.
3. Ticket numbers are unique and never reused during the process.
4. A slot must exist and be occupied before it can be released.
5. EVs prefer charging bays; only vehicles requiring charging can use a charger-only bay.
6. A negative duration, invalid registration, invalid pass period or invalid slot is rejected.
7. A valid pass covering the parking date makes the parking charge zero; charging consumption is handled separately in this implementation.
8. Every floor may have a different number of slots.
9. The sample rates are illustrative because the PDF gives fee examples, not a complete official tariff.

### Alternative: objects vs parallel arrays
With objects, one `Vehicle` stores its registration, category and behaviour. Searching a simple array of n vehicles is O(n); a HashMap keyed by registration gives average O(1). Adding a new field requires changing one class and its constructors/getters. With parallel arrays (`regNo[]`, `type[]`, `entryTime[]`), search is also O(n), but each index must remain aligned across all arrays. Adding an attribute requires another array and updates at every insertion, deletion and copy site. Both use O(n) total storage; objects have per-object header/reference overhead, while parallel arrays store fields separately and may use less object overhead.

---

## Task 2 – Choose the Data Representation (3 marks)

| Data | Java representation | Reason |
|---|---|---|
| Floor and slot index | `int` | Whole-number indexes |
| Registration number | `String` | Alphanumeric identifier |
| Entry/exit time | `LocalDateTime` | Date and time without lossy string parsing |
| Duration | `Duration` / `long` minutes | Accurate elapsed time |
| Fee/rate | `double` for this teaching example; production billing should use `BigDecimal` | Supports fractional currency calculations |
| Occupant | `Vehicle` reference or null | Stores a polymorphic object |

A jagged array `Slot[][]` is selected because each floor can have a different number of bays. A regular two-dimensional array assumes equal row lengths. A one-dimensional array is possible only with prefix offsets or a fixed slots-per-floor formula. The formula `floor * slotsPerFloor + slot` fails when floors have unequal capacities unless a prefix-offset table is also maintained. `ArrayList<Slot[]>` supports variable rows but has a small collection/reference overhead. All representations use O(S) space for S total slots; direct index access is O(1), while scanning for a free slot is O(S).

### Casting and precision example
`130 / 60` using integer operands equals 2, so charging `2 * 30 = ₹60` would undercharge a started-hour tariff. The correct billable-hour calculation is ceiling division using integer arithmetic: `minutes / 60 + (minutes % 60 == 0 ? 0 : 1)`, giving 3 hours and ₹90. Avoid casting the result to int after floating-point division when exact integer ceiling logic is available.

Input can be read with `Scanner` for a console demonstration; a deployed application would receive validated values from a UI/API.

---

## Task 3 – Design the Control Logic (4 marks)

### Pseudocode: allocate a slot
```text
ALGORITHM AllocateSlot(vehicle)
    IF vehicle is invalid THEN
        RETURN INVALID_INPUT
    END IF
    FOR each floor f from first to last DO
        FOR each slot s in floor f DO
            IF slot is free AND slot is suitable for vehicle THEN
                Mark slot occupied by vehicle
                RETURN (f, s)
            END IF
        END FOR
    END FOR
    RETURN NO_SLOT
END ALGORITHM
```

### Pseudocode: compute fee
```text
ALGORITHM CalculateFee(vehicle, entryTime, exitTime, pass)
    IF exitTime is before entryTime THEN
        RETURN INVALID_DURATION
    END IF
    IF pass is valid for the complete parking date THEN
        parkingFee <- 0
    ELSE
        minutes <- elapsed whole minutes
        billableHours <- minutes / 60
        IF minutes MOD 60 is not 0 THEN
            billableHours <- billableHours + 1
        END IF
        parkingFee <- billableHours * vehicle hourly rate
        IF parkingFee exceeds daily maximum THEN
            parkingFee <- daily maximum
        END IF
    END IF
    RETURN parkingFee + applicable charging fee
END ALGORITHM
```

The search stops immediately with `return` when a suitable slot is found. `break` can leave a loop once a match is found; `continue` skips unsuitable occupied slots. If no slot is found, return null or report a clear “No suitable slot” result. Fee slabs can be implemented using if–else or a strategy object.

### Completed supplied code
`Task03/ParkingFacility.java` fills both TODOs and preserves the supplied main method. Expected output:
```text
[0, 0]
[0, 1]
[1, 0]
null
true false false
[0, 1]
```

### Alternative: free-slot stacks
Maintain one stack of free slot identifiers per floor and a free count. Allocation pops from a suitable floor; release pushes the released slot back. With a stack and suitable-floor selection, push/pop are O(1); finding a suitable floor is O(F), or O(1) with an additional index/queue. The scan implementation is O(S) worst case for S total slots and O(1) auxiliary space; stacks require O(S) extra slot references but make allocation/release faster.

---

## Task 4 – Design Classes, Objects and Constructors (4 marks)

The model uses constructors to establish valid state immediately. For example, `Vehicle(regNo, pricingRule)` validates registration and requires a non-null pricing policy; `Ticket(vehicle, floor, slot, time)` validates its vehicle, coordinates and timestamp. Subclass constructors call `super(...)`. Overloaded ticket constructors can supply the current time by default or accept an explicit time for testing. `this(...)` constructor chaining avoids repeating validation. A copy constructor/copy method is useful only when a snapshot is required; a copied ticket should normally receive a new unique ticket number rather than duplicate an active identifier.

A local reference variable is typically held in a stack frame; the object it references is allocated on the heap. Passing a vehicle to a method passes a copy of the reference, not a copy of the object.

### Alternative construction styles
- Default constructor plus setters permits temporarily incomplete objects and requires validation in every setter/call sequence.
- Parameterised constructors validate once and provide an immutable, valid object.
- Static factory methods can give meaningful creation names and select a subclass/rule.

Creating n tickets is O(n) time and O(n) total object storage in all three approaches. Constructor chaining does not change asymptotic complexity; factory methods add only constant per-object dispatch/validation overhead. The key difference is correctness and maintainability, not Big-O.

---

## Task 5 – Apply static Members and the Object Life Cycle (3 marks)

The ticket sequence is class-wide data, so it belongs in a static counter such as `AtomicLong NEXT`. Each ticket has its own ticket number, vehicle, entry time and slot. A static utility method may validate a registration or calculate a shared conversion; a static initialisation block is appropriate only for one-time class configuration.

After exit, the ticket is removed from the active-ticket map and no longer referenced by the service. Once no reachable reference points to it, it becomes eligible for garbage collection; Java does not promise the exact collection time. Objects are passed and returned as references.

### Alternative: instance counter
If each facility owns an instance counter, north and south facilities can both issue ticket number 1001, causing collisions if numbers must be globally unique. A static counter uses O(1) counter storage for all k facilities and n tickets; an instance counter uses O(k) counter storage. The ticket objects themselves require O(n) storage while retained.

---

## Task 6 – Design the Inheritance Hierarchy (4 marks)

The base `Vehicle` is abstract. `TwoWheeler` and `Car` extend it (hierarchical inheritance); `ElectricCar` extends `Car` (multilevel inheritance) and implements `Chargeable`. Common state such as registration belongs in the base class; category-specific pricing/capability belongs in subclasses or composed policies. A user hierarchy could similarly have `CampusUser` with `Student`, `Staff` and `Visitor`, but pass validity is modelled independently to avoid mixing identity and payment policy.

`super(regNo)` invokes the parent constructor and `super.method()` can invoke parent behaviour. ElectricCar construction order is Vehicle → Car → ElectricCar (base constructor first, then each subclass body).

### UML (text form)
```text
                 Vehicle (abstract)
                 /             \
          TwoWheeler            Car
                                   |
                              ElectricCar ---- Chargeable
```

### Alternative: composition
A single Vehicle can contain a `VehicleType` or `PricingRule` object. Adding a type then means creating a new rule object rather than another subclass. Inheritance has O(1) virtual dispatch and no separate strategy reference if rate is encoded in the class; composition also has O(1) method dispatch but stores a policy reference per vehicle (or shares policy instances). Composition is more flexible when rates change independently of vehicle identity.

---

## Task 7 – Override and Specialise Behaviour (4 marks)

Subclasses specialise `ratePerHour()` or their pricing rule. The `@Override` annotation lets the compiler check that the method actually overrides a parent method. An overriding method must keep a compatible signature and covariant return type, and cannot reduce access visibility. `final` is suitable for immutable registration/ticket identifiers and methods whose invariant must not be changed; a class should be final only when extension is intentionally prohibited.

`toString()` gives a readable vehicle description. `equals()` compares registration numbers, and `hashCode()` uses the same registration field. Thus equal vehicles must have equal hash codes. Registration is immutable to avoid corrupting hash-based collections after insertion.

### Alternative: switch on a type field
A single method could switch on a vehicle-type enum and select a rate. The switch is O(1) for a finite enum in typical implementations, just like virtual dispatch asymptotically. However, every new category requires editing the central switch and retesting it. With overriding/strategy, the new subclass/rule can be added without changing existing fee classes.

---

## Task 8 – Apply Polymorphism (4 marks)

Compile-time polymorphism is method overloading, such as `calculateFee(int minutes)` and `calculateFee(LocalDateTime in, LocalDateTime out)`; the compiler chooses based on argument types and count. Runtime polymorphism occurs when a `Vehicle` reference refers to a `Car` or `TwoWheeler`; dynamic dispatch invokes the actual object's overridden implementation.

Trace: declared reference type = `Vehicle`; actual object = `ElectricCar`; call = `calculateFee(130)`; executed implementation = inherited Vehicle method using the ElectricCar/Car pricing behaviour. An array of Vehicle references can contain all vehicle subclasses (upcasting), avoiding a long type-checking chain.

### Completed supplied code
`Task08/FeeDemo.java` completes the fee calculation, Car constructor/rate and ElectricCar charging fee. Expected output:
```text
TwoWheeler TN01A1 -> Rs 30.0
Car TN02B2 -> Rs 90.0
ElectricCar TN03C3 -> Rs 210.0
true
```
The final line is true because equality is defined by registration number. This is acceptable if registration identifies one vehicle independent of its subtype; if subtype is part of identity, equals must also compare category.

### Alternative: instanceof and casting
A chain of up to k checks may be needed for k categories, whereas virtual dispatch selects the implementation directly. Both are constant for fixed k, but the chain grows with each category and centralises changes in one method.

---

## Task 9 – Introduce Abstraction (4 marks)

`Vehicle` is abstract because a generic vehicle cannot be priced without a concrete category/rule. `PricingRule` is an interface because different independent algorithms can calculate fees. `Chargeable` is a capability interface because only some vehicles support energy charging. A class can implement several interfaces, allowing an EV to be both a Car and Chargeable. Interface default methods are useful for shared optional behaviour; static methods are useful for interface-related helper functions.

### Alternative abstraction reversal
If Vehicle were an interface, it could not hold ordinary per-object instance fields with constructor initialisation; each implementation would need to manage registration state. A class can implement multiple interfaces, but cannot extend multiple abstract classes. Making Chargeable an abstract class would prevent an EV from also extending Car because Java permits only one superclass. Method-call complexity remains O(1) for both interface and abstract-class dispatch; per-object state is determined by the implementing class fields, not the interface itself.

---

## Task 10 – Packages and Encapsulation (3 marks)

Recommended package layout:
```text
parking/
  model/       Vehicle, Ticket, Slot, ParkingPass
  pricing/     PricingRule, HourlyRule, FlatRule, VanRule
  service/     ParkingService, ParkingFacility
  app/         Main/demo and tests
```

Fields are private to protect invariants. Constructors and public service methods form the controlled API. Protected members are used only for subclass extension; package-private access is reserved for tightly related internal classes. Getters expose read-only values. Setters should reject negative durations, invalid rates and invalid coordinates. Immutable fields are preferred for registration and ticket identifiers.

Wrapper types such as `Integer` can be autoboxed when stored in generic collections. Do not compare wrapper values with `==`; use `equals()` or unbox safely, because `==` may compare references and null can cause unboxing errors.

### Alternative: public fields
Public fields let callers bypass validation and can create impossible states (negative rates, duplicate IDs, invalid slots). Encapsulation centralises validation. A validation check is O(1) per update; repeating the same check in c calling locations costs O(c) maintenance effort and risks inconsistent rules.

---

## Task 11 – Implement, Test and Defend the Design (5 marks)

The integrated program is `src/SmartParkingSystem.java`. It includes a jagged floor structure, slot suitability, active-ticket map, unique ticket counter, vehicle hierarchy, pricing-rule abstraction, charging capability and passes.

### Test table
| Test | Expected | Program check |
|---|---|---|
| Enter vehicle | Unique ticket | PASS |
| Duplicate active registration | Reject | PASS |
| Car stays 60 minutes | ₹30 at sample rate | PASS |
| Zero-minute stay | ₹0 | PASS |
| EV allocation | Charging bay preferred | PASS |
| EV 61 minutes + 2 kWh | ₹84 (₹60 + ₹24) | PASS |
| Valid monthly pass | Parking fee ₹0 | PASS |
| Valid visitor one-day pass | Parking fee ₹0 | PASS |
| Invalid floor/slot | Reject release | PASS |
| Allocate final free slot | Succeeds | PASS |
| Allocate when full | No slot/null | PASS |
| E-bike flat fee | ₹15 | PASS |
| Delivery van 10:00, 60 minutes | ₹60 | PASS |
| Delivery van 18:00, 60 minutes | ₹30 | PASS |

Daily collection example: car for 60 minutes = ₹30; EV for 61 minutes plus 2 kWh = ₹84; total = ₹114. The test program checks these independently. The pass cases collect ₹0 for parking.

The design uses encapsulation (private fields), inheritance (vehicle categories), polymorphism (vehicle references and pricing policies), and abstraction (interfaces and abstract Vehicle). One limitation is that the sample tariff and pass policy are simplified; production rules should use configurable rates, explicit user eligibility, robust date/time-zone handling and currency-safe `BigDecimal`.

For n active vehicles, an array scan for exit lookup is O(n); an ArrayList is also O(n) for search. A HashMap keyed by registration gives average O(1) lookup and O(n) storage, so it is used by the integrated service. An array/list may have lower per-entry overhead but slower lookup.

---

## Task 12 – Re-evaluate and Adapt the Design (8 marks)

### A. Impact analysis
The changed requirements add e-bikes with flat pricing, delivery vans with double car rates during 09:00–17:00, visitor one-day passes and five unequal floors. The slot structure must support variable row lengths, so a jagged array remains appropriate. New vehicle categories, pass types and pricing policies are affected; the allocation service should not contain category-specific tariff switches.

### B. Evaluate original design
A design with vehicle subclasses and an independent pricing abstraction can add categories without rewriting the existing vehicle hierarchy. If pricing rules are embedded in one large switch or spread through UI/service code, each new category would force edits across several classes. The revised model keeps tariff behaviour in PricingRule implementations and pass validity in ParkingPass implementations.

### C. Adaptation
The design is **modified**, not completely redesigned:
- Add `EBike` with a flat pricing rule.
- Add `DeliveryVan` with a time-window pricing rule.
- Add `OneDayPass` implementing the pass interface.
- Keep `MonthlyPass` for eligible monthly-pass holders.
- Retain jagged `Slot[][]` floors with per-floor capacities and charger counts.
- Keep the same `ParkingService` entry/exit API.

### D. Evidence-based defence
The integrated demo tests both new vehicle categories, one-day pass validity, monthly pass validity, unequal floor capacities, charging-bay preference and normal allocation/release. The original service and ticket concepts remain stable; extension occurs through new Vehicle/PricingRule/Pass implementations. The trade-off is that pricing rules need careful boundary tests (especially a stay crossing 09:00 or 17:00); the sample implementation selects the van multiplier using the entry time as a simplified policy assumption.

---

## Final conclusion

The campus smart-parking system is modelled using object-oriented Java concepts from CO1. The completed supplied programs demonstrate slot allocation, release, inheritance, overriding, polymorphism and interfaces. The integrated implementation extends the design to support passes, charging, additional vehicle types and unequal floor capacities. The test output provides evidence for the normal and boundary cases listed above.
