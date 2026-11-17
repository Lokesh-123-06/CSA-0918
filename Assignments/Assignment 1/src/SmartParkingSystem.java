import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/** Integrated CO1 Smart Parking implementation. Compile with Java 17+. */
public class SmartParkingSystem {
    interface PricingRule { double fee(Duration stay, LocalTime entry); }
    interface ParkingPass { boolean validOn(LocalDate date); }
    interface Chargeable { double chargingFee(int kWh); }

    static final class HourlyRule implements PricingRule {
        private final double rate, dailyCap;
        HourlyRule(double rate, double dailyCap) {
            if (rate < 0 || dailyCap < 0) throw new IllegalArgumentException("Invalid rate");
            this.rate = rate; this.dailyCap = dailyCap;
        }
        public double fee(Duration stay, LocalTime entry) {
            long m = Math.max(0, stay.toMinutes());
            long hours = (m + 59) / 60;
            double amount = hours * rate;
            return dailyCap > 0 ? Math.min(amount, dailyCap) : amount;
        }
    }
    static final class FlatRule implements PricingRule {
        private final double amount;
        FlatRule(double amount) { if (amount < 0) throw new IllegalArgumentException(); this.amount = amount; }
        public double fee(Duration stay, LocalTime entry) { return stay.isZero() ? 0 : amount; }
    }
    static final class VanRule implements PricingRule {
        private final double carRate;
        VanRule(double carRate) { this.carRate = carRate; }
        public double fee(Duration stay, LocalTime entry) {
            long hours = (Math.max(0, stay.toMinutes()) + 59) / 60;
            boolean business = entry != null && !entry.isBefore(LocalTime.of(9, 0))
                    && entry.isBefore(LocalTime.of(17, 0));
            return hours * carRate * (business ? 2 : 1);
        }
    }

    static abstract class Vehicle {
        private final String regNo;
        private final PricingRule rule;
        protected Vehicle(String regNo, PricingRule rule) {
            if (regNo == null || regNo.isBlank()) throw new IllegalArgumentException("Registration is required");
            this.regNo = regNo.trim().toUpperCase(Locale.ROOT);
            this.rule = Objects.requireNonNull(rule);
        }
        public final String regNo() { return regNo; }
        public final double calculateFee(Duration stay, LocalTime entry) { return rule.fee(stay, entry); }
        public final double calculateFee(int minutes) {
            if (minutes < 0) throw new IllegalArgumentException("Negative duration");
            return calculateFee(Duration.ofMinutes(minutes), LocalTime.MIDNIGHT);
        }
        public abstract String category();
        public boolean needsCharger() { return false; }
        @Override public String toString() { return category() + "[" + regNo + "]"; }
        @Override public boolean equals(Object o) { return o instanceof Vehicle v && regNo.equals(v.regNo); }
        @Override public int hashCode() { return regNo.hashCode(); }
    }
    static class TwoWheeler extends Vehicle {
        TwoWheeler(String id) { super(id, new HourlyRule(10, 100)); }
        public String category() { return "TwoWheeler"; }
    }
    static class Car extends Vehicle {
        Car(String id) { super(id, new HourlyRule(30, 300)); }
        public String category() { return "Car"; }
    }
    static class ElectricCar extends Car implements Chargeable {
        ElectricCar(String id) { super(id); }
        public String category() { return "ElectricCar"; }
        public boolean needsCharger() { return true; }
        public double chargingFee(int kWh) {
            if (kWh < 0) throw new IllegalArgumentException("Negative kWh");
            return kWh * 12.0;
        }
    }
    static class EBike extends Vehicle {
        EBike(String id) { super(id, new FlatRule(15)); }
        public String category() { return "EBike"; }
    }
    static class DeliveryVan extends Vehicle {
        DeliveryVan(String id) { super(id, new VanRule(30)); }
        public String category() { return "DeliveryVan"; }
    }

    static final class MonthlyPass implements ParkingPass {
        private final LocalDate from, until;
        MonthlyPass(LocalDate from, LocalDate until) {
            if (from == null || until == null || until.isBefore(from)) throw new IllegalArgumentException("Invalid dates");
            this.from = from; this.until = until;
        }
        public boolean validOn(LocalDate d) { return d != null && !d.isBefore(from) && !d.isAfter(until); }
    }
    static final class OneDayPass implements ParkingPass {
        private final LocalDate day;
        OneDayPass(LocalDate day) { this.day = Objects.requireNonNull(day); }
        public boolean validOn(LocalDate d) { return day.equals(d); }
    }
    static final class Slot {
        private final boolean charger;
        private Vehicle occupant;
        Slot(boolean charger) { this.charger = charger; }
        boolean free() { return occupant == null; }
        boolean canFit(Vehicle v) { return free() && (!charger || v.needsCharger()); }
        void occupy(Vehicle v) { if (!canFit(v)) throw new IllegalStateException("Slot unavailable"); occupant = v; }
        void release() { if (free()) throw new IllegalStateException("Slot already free"); occupant = null; }
    }
    static final class Ticket {
        private static final AtomicLong NEXT = new AtomicLong(1000);
        private final long number = NEXT.getAndIncrement();
        private final Vehicle vehicle;
        private final int floor, slot;
        private final LocalDateTime entry;
        Ticket(Vehicle v, int f, int s) { this(v, f, s, LocalDateTime.now()); }
        Ticket(Vehicle v, int f, int s, LocalDateTime time) {
            vehicle = Objects.requireNonNull(v);
            if (f < 0 || s < 0 || time == null) throw new IllegalArgumentException("Invalid ticket");
            floor = f; slot = s; entry = time;
        }
        long number() { return number; }
        Vehicle vehicle() { return vehicle; }
        int floor() { return floor; }
        int slot() { return slot; }
        LocalDateTime entry() { return entry; }
        Ticket copy() { return new Ticket(vehicle, floor, slot, entry); }
        public String toString() { return "Ticket#" + number + " " + vehicle + " [" + floor + "," + slot + "]"; }
    }

    static final class Facility {
        private final Slot[][] floors;
        Facility(int[] slotCounts, int[] chargerCounts) {
            if (slotCounts == null || chargerCounts == null || slotCounts.length == 0
                    || slotCounts.length != chargerCounts.length) throw new IllegalArgumentException("Invalid floors");
            floors = new Slot[slotCounts.length][];
            for (int f = 0; f < slotCounts.length; f++) {
                if (slotCounts[f] < 0 || chargerCounts[f] < 0 || chargerCounts[f] > slotCounts[f])
                    throw new IllegalArgumentException("Invalid slot configuration");
                floors[f] = new Slot[slotCounts[f]];
                for (int s = 0; s < slotCounts[f].length; s++)
                    floors[f][s] = new Slot(s < chargerCounts[f]);
            }
        }
        synchronized int[] allocate(Vehicle v) {
            if (v == null) throw new IllegalArgumentException("Vehicle required");
            if (v.needsCharger()) {
                int[] p = scan(v, true);
                if (p != null) return p;
            }
            return scan(v, false);
        }
        private int[] scan(Vehicle v, boolean chargerOnly) {
            for (int f = 0; f < floors.length; f++) for (int s = 0; s < floors[f].length; s++) {
                Slot slot = floors[f][s];
                if (slot.free() && (!chargerOnly || (s < chargerCount(f))) && slot.canFit(v)) {
                    slot.occupy(v); return new int[]{f, s};
                }
            }
            return null;
        }
        private int chargerCount(int f) {
            int count = 0; for (Slot slot : floors[f]) if (slot.charger) count++; return count;
        }
        synchronized boolean release(int f, int s) {
            if (f < 0 || f >= floors.length || s < 0 || s >= floors[f].length || floors[f][s].free()) return false;
            floors[f][s].release(); return true;
        }
        synchronized int occupied() {
            int n = 0; for (Slot[] row : floors) for (Slot slot : row) if (!slot.free()) n++; return n;
        }
    }
    static final class ParkingService {
        private final Facility facility;
        private final Map<String, Ticket> active = new HashMap<>();
        ParkingService(Facility facility) { this.facility = Objects.requireNonNull(facility); }
        synchronized Ticket enter(Vehicle v) {
            if (active.containsKey(v.regNo())) throw new IllegalArgumentException("Duplicate active registration");
            int[] p = facility.allocate(v);
            if (p == null) throw new IllegalStateException("No suitable slot available");
            Ticket t = new Ticket(v, p[0], p[1]); active.put(v.regNo(), t); return t;
        }
        synchronized double exit(String regNo, LocalDateTime out, ParkingPass pass, int kWh) {
            Ticket t = active.get(regNo.toUpperCase(Locale.ROOT));
            if (t == null || out == null || out.isBefore(t.entry())) throw new IllegalArgumentException("Invalid exit");
            boolean free = pass != null && pass.validOn(t.entry().toLocalDate()) && pass.validOn(out.toLocalDate());
            double amount = 0;
            if (!free) {
                amount = t.vehicle().calculateFee(Duration.between(t.entry(), out), t.entry().toLocalTime());
                if (t.vehicle() instanceof Chargeable c) amount += c.chargingFee(kWh);
            }
            if (!facility.release(t.floor(), t.slot())) throw new IllegalStateException("Could not release slot");
            active.remove(t.vehicle().regNo()); return amount;
        }
        synchronized Ticket find(String regNo) { return active.get(regNo.toUpperCase(Locale.ROOT)); }
        synchronized int activeCount() { return active.size(); }
    }

    private static void check(String name, boolean ok) {
        System.out.println(name + ": " + (ok ? "PASS" : "FAIL"));
        if (!ok) throw new AssertionError(name);
    }
    public static void main(String[] args) {
        // Five unequal floors demonstrate the revised facility representation.
        Facility facility = new Facility(new int[]{3, 4, 2, 5, 3}, new int[]{1, 1, 0, 2, 1});
        ParkingService service = new ParkingService(facility);
        Ticket bike = service.enter(new TwoWheeler("TN01AA0001"));
        check("Entry ticket issued", bike.number() >= 1000);
        boolean duplicate = false;
        try { service.enter(new Car("TN01AA0001")); } catch (IllegalArgumentException ex) { duplicate = true; }
        check("Duplicate registration rejected", duplicate);
        Ticket car = service.enter(new Car("TN02BB0002"));
        check("One-hour car fee is Rs 30", service.exit("TN02BB0002", car.entry().plusMinutes(60), null, 0) == 30);
        check("Zero-minute bike fee is zero", service.exit("TN01AA0001", bike.entry(), null, 0) == 0);
        Ticket ev = service.enter(new ElectricCar("TN03CC0003"));
        check("EV prefers a charging bay", ev.floor() == 0 && ev.slot() == 0);
        check("EV 61 minutes plus 2 kWh costs Rs 84",
                service.exit("TN03CC0003", ev.entry().plusMinutes(61), null, 2) == 84);
        Ticket monthly = service.enter(new Car("TN04DD0004"));
        LocalDate day = monthly.entry().toLocalDate();
        check("Monthly pass is free", service.exit("TN04DD0004", monthly.entry().plusHours(3),
                new MonthlyPass(day.minusDays(1), day.plusDays(10)), 0) == 0);
        Ticket visitor = service.enter(new Car("TN05EE0005"));
        check("Visitor one-day pass is free", service.exit("TN05EE0005", visitor.entry().plusHours(2),
                new OneDayPass(visitor.entry().toLocalDate()), 0) == 0);
        check("Invalid slot rejected", !facility.release(99, 0));
        Facility small = new Facility(new int[]{1}, new int[]{0});
        check("Last free slot allocated", small.allocate(new TwoWheeler("TN06FF0006")) != null);
        check("Full facility returns null", small.allocate(new Car("TN07GG0007")) == null);
        check("E-bike flat fee is Rs 15",
                new EBike("TN08HH0008").calculateFee(Duration.ofMinutes(20), LocalTime.NOON) == 15);
        check("Van business-hour fee is Rs 60",
                new DeliveryVan("TN09JJ0009").calculateFee(Duration.ofMinutes(60), LocalTime.of(10, 0)) == 60);
        check("Van after-hours fee is Rs 30",
                new DeliveryVan("TN10KK0010").calculateFee(Duration.ofMinutes(60), LocalTime.of(18, 0)) == 30);
        check("All active tickets exited", service.activeCount() == 0 && facility.occupied() == 0);
        System.out.println("All smart-parking tests passed.");
    }
}