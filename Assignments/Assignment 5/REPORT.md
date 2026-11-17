# Assignment 5 – CO5 Data Persistence Engineering

## PART A – Persistence Design & Pseudocode (25)

### A1 – File format (7)
Text record format: passId, registerNo, escapedName, routeNo, boardingStop, ISO-8601 validity date, amount. Use UTF-8, a delimiter with CSV-style quoting/escaping, reject newline injection and validate exact field count. Binary DataOutputStream layout: writeUTF(passId), writeUTF(regNo), writeUTF(name), writeInt(route), writeUTF(stop), writeLong(validity.toEpochDay()), writeDouble(amount); DataInputStream reads in the same order. For text, BufferedReader/BufferedWriter provides line-oriented buffered I/O; FileReader/FileWriter use platform default charset unless explicitly wrapped with InputStreamReader/OutputStreamWriter UTF-8. Add: validate → append complete escaped line → flush/close. Find: scan each valid line and compare pass ID. List route: scan and select route matches. Damaged line: validate field count/types, log and skip/quarantine. Missing file: report missing/initialise empty file according to operation. Power failure: append to temp file then atomic replace for full rewrite, or use journal/checksum and newline-terminated records for append; a final partial line is rejected. RandomAccessFile fixed-length record index gives O(1) seek only if an ID→record-index index exists; otherwise finding ID still O(n) scan. Padding wastes unused bytes; fixed-size record stores simplify offsets.

### A2 – Buffered I/O (5)
A 50 MB file read one byte at a time may invoke approximately 52 million Java read operations; buffering reads blocks (e.g. 8–64 KB), reducing underlying I/O calls to roughly fileSize/bufferSize. Use BufferedInputStream/BufferedReader or a byte array loop. Buffering helps sequential I/O; it may add little when data is already in memory/cache or access is random and tiny. Files.readAllLines uses O(file size) memory and is convenient for small files; buffered streaming uses O(buffer size) memory and is safer for 50 MB or larger.

### A3 – DAO pattern (5)
Common flow: acquire connection, prepare SQL, bind parameters, execute, map ResultSet, close resources. Table/columns and mapping differ. Define Dao<T,K> with insert, findById, findAll, update, delete; StudentDao implements it. Student fields map to student_id primary key, name, email, course_id. SQL in service/GUI duplicates logic and spreads schema changes across callers; DAO centralises changes. Runtime complexity per query is governed by DB/index and is effectively the same; abstraction adds negligible constant overhead.

### A4 – Serialization (4)
Serializable object graph traverses Hostel→Rooms→Residents. Mark live Connection and password transient; a Connection cannot be safely persisted, and passwords should not be stored in plaintext. Persist only needed resident data; use encrypted credential storage or omit secrets. New phone field in old serialized files receives default null/zero when compatible; explicit stable serialVersionUID allows compatible evolution but incompatible structural changes can cause InvalidClassException. CSV/JSON is more portable and human-readable, but needs explicit schema mapping; both are O(n) save/load and O(n) file/object memory, with text generally larger but easier to evolve with optional fields.

### A5 – Transactions and injection (4)
Set autoCommit(false); debit source only if balance≥500; credit destination; commit both; on SQLException rollback and rethrow; restore original auto-commit in finally. Auto-commit independent updates can debit without credit if failure occurs between them. Transaction overhead is small relative to network/commit latency but protects atomicity. SQL concatenation is injectable: crafted id such as `' OR '1'='1' -- ` can alter the predicate. Use PreparedStatement placeholders for id and password; never concatenate user input.

## PART B – Implementation & Experimental Investigation (25)

### B1 – Instrumented file store (7)
PassStore uses UTF-8 buffered lines, validates exactly five fields and skips damaged lines; add/find/list are demonstrated. File search is O(n) per lookup. A startup HashMap index costs O(n) load and O(n) memory, then average O(1) lookups; for q searches cost changes from O(qn) to O(n+q). Buffered line counting reduces system calls; measure unbuffered vs buffered for 1/10/50 MB. File find over 1k/10k/100k records should scale approximately linearly.

### B2 – Adversarial inputs (5)
Unsafe login concatenation is vulnerable to SQL injection; PreparedStatement binds data values and prevents them from changing SQL structure. Test damaged lines with too few fields, empty line and nonnumeric amount; validation rejects/quarantines them and continues. Regex escaping is weaker because SQL dialects/encodings and edge cases make complete escaping difficult; prepared statements separate SQL from data. Validation is O(length) per input but does not replace parameter binding.

### B3 – Statement vs PreparedStatement vs batch (8)
Statement parses SQL text repeatedly and is unsafe with concatenated values. PreparedStatement compiles parameterised SQL once and binds values repeatedly. Batch groups parameter sets to reduce round trips; process batches of 1,000 inside one transaction and rollback on any failure. For one pass at a counter, PreparedStatement is simplest; nightly 100k import should use batched PreparedStatement or LOAD DATA after validation. Transaction mode gives atomicity; auto-commit may preserve partial rows after injected failure. Compare 10, 1k, 10k and 100k records, inspect ResultSetMetaData and report median of repeated trials.

### B4 – Engineering decision (5)
For 12 campuses, use a central relational database with unique constraints and transactions, not independent files/serialized copies. Migration reads and validates each file row, batches valid rows, records rejects with line numbers and commits transactionally. DatabaseMetaData reports product, driver and table names to diagnose deployment differences. One shared database provides concurrent multi-counter safety; campus files create synchronization, search and reconciliation problems. Search with indexed pass_id is O(log n) B-tree or near O(1) hash-index expected by engine; scanning 12 files is O(n) total.

### Measurement protocol
Run each benchmark after warm-up, repeat at least five times, report median and mean, file sizes/row counts, Java version, database version, driver version and machine. Record actual values in MEASUREMENTS.csv. Never substitute invented timings for execution evidence.
