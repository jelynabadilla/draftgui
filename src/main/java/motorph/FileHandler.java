package motorph;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*; 
// import java.util.stream.*; // Not explicitly used now, but good to have for future stream operations

public class FileHandler {

    private static final String DATA_FOLDER = "data";
    private static final String EMPLOYEE_FILE = DATA_FOLDER + File.separator + "employees.csv";
    private static final String ATTENDANCE_FILE = DATA_FOLDER + File.separator + "attendance.csv";
    private static final List<DateTimeFormatter> TIME_FORMATTERS = Arrays.asList(
        DateTimeFormatter.ofPattern("HH:mm"), // For two-digit hour format like "08:05"
        DateTimeFormatter.ofPattern("H:mm")   // For single-digit hour format like "8:05"
    );

    // CSV file headers
    public static final String EMPLOYEE_HEADER = "Employee #,Last Name,First Name,Birthday,Address,Phone Number,SSS #,Philhealth #,TIN #,Pag-ibig #,Status,Position,Immediate Supervisor,Basic Salary,Rice Subsidy,Phone Allowance,Clothing Allowance,Gross Semi-monthly Rate,Hourly Rate";
    public static final String ATTENDANCE_HEADER = "Employee #,Last Name,First Name,Date,Log In,Log Out";
    private static final DateTimeFormatter EMPLOYEE_BIRTHDAY_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");


    public FileHandler() {
        try {
            //This ensures that the data folder exists
            Files.createDirectories(Paths.get(DATA_FOLDER));
            //Creates a CSV file with headers if they don't exist
            ensureFileExists(EMPLOYEE_FILE, EMPLOYEE_HEADER);
            ensureFileExists(ATTENDANCE_FILE, ATTENDANCE_HEADER);
        } catch (IOException e) {
            //Prints an error message if directory creation fails
            System.err.println("Error creating data directory: " + e.getMessage());
        }
    }

    //This ensures that a file exists, and creates it with a header if it does not
    private void ensureFileExists(String filePath, String header) {
        File file = new File(filePath);
        //Checks if file doesn't exist
        if (!file.exists()) {
            //Try-with-resources to ensure CSVWriter is closed automatically
            try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {
                //Writes the header to the new file
                writer.writeNext(header.split(","));
            } catch (IOException e) {
                //Prints an error message if file creation fails
                System.err.println("Error creating file " + filePath + ": " + e.getMessage());
            }
        }
    }

    //Reads all employee data from the CSV file
    public List<Employee> readEmployees() {
        List<Employee> employees = new ArrayList<>();
        String[] headers = EMPLOYEE_HEADER.split(","); // Get headers for mapping
        //Try-with-resources to ensure CSVReader is closed automatically
        try (CSVReader reader = new CSVReader(new FileReader(EMPLOYEE_FILE))) {
            String[] nextLine;
            //Skips the header line
            reader.readNext();
            //Loops through each line in the CSV
            while ((nextLine = reader.readNext()) != null) {
                //Ensure the line has enough columns before accessing them
                if (nextLine.length >= headers.length) { // Check against number of headers
                    Map<String, String> employeeDataMap = new HashMap<>();
                    for (int i = 0; i < headers.length; i++) {
                        employeeDataMap.put(headers[i], nextLine[i]);
                    }
                    Employee employee = new Employee(employeeDataMap);
                    employees.add(employee); //Adds employee to the list
                } else {
                    //Log or handle lines that don't have enough columns
                    System.err.println("Skipping malformed line in employees.csv (not enough columns for headers): " + String.join(",", nextLine));
                }
            }
        } catch (IOException | CsvValidationException e) {
            //Prints an error message if reading fails
            System.err.println("Error reading employees file: " + e.getMessage());
            e.printStackTrace(); // Added for more detail
        } catch (Exception e) { // Catch any other exceptions during employee creation (e.g. from Employee constructor)
            System.err.println("Error processing employee data line: " + e.getMessage());
            e.printStackTrace(); // Added for more detail
        }
        return employees; //Returns the list of employees
    }


    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0; // Default value if parsing fails
        }
    }

    //Saves all employees to the CSV file, overwriting existing content
    public void saveAllEmployees(List<Employee> employees) {
        //Try-with-resources to ensure CSVWriter is closed automatically
        try (CSVWriter writer = new CSVWriter(new FileWriter(EMPLOYEE_FILE, false))) { // false to overwrite
            //Writes the header first
            writer.writeNext(EMPLOYEE_HEADER.split(","));
            //Writes each employee's data to the CSV
            for (Employee emp : employees) {
                String birthdayString;
                Object rawBirthday = emp.getBirthday(); 
                if (rawBirthday instanceof LocalDate) {
                    birthdayString = ((LocalDate) rawBirthday).format(EMPLOYEE_BIRTHDAY_FORMATTER);
                } else if (rawBirthday != null) {
                    birthdayString = rawBirthday.toString(); 
                } else {
                    birthdayString = ""; 
                }

                writer.writeNext(new String[]{
                    emp.getEmployeeId(), emp.getLastName(), emp.getFirstName(), birthdayString, 
                    emp.getAddress(), emp.getPhoneNumber(), emp.getSssNumber(), emp.getPhilhealthNumber(),
                    emp.getTinNumber(), emp.getPagibigNumber(), emp.getStatus(), emp.getPosition(),
                    emp.getSupervisor(), 
                    String.valueOf(emp.getBasicSalary()), String.valueOf(emp.getRiceSubsidy()),
                    String.valueOf(emp.getPhoneAllowance()), String.valueOf(emp.getClothingAllowance()),
                    String.valueOf(emp.getGrossRate()), 
                    String.valueOf(emp.getHourlyRate()) 
                });
            }
        } catch (IOException e) {
            //Prints an error message if saving fails
            System.err.println("Error saving employees file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    //Retrieves all attendance records from the CSV file
    public List<Attendance> getAllAttendanceRecords() {
        List<Attendance> records = new ArrayList<>();
        //DateTimeFormatter for parsing date from CSV
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");

        //Try-with-resources to ensure CSVReader is closed automatically
        try (CSVReader reader = new CSVReader(new FileReader(ATTENDANCE_FILE))) {
            String[] nextLine;
            reader.readNext(); // Skip header line

            while ((nextLine = reader.readNext()) != null) {
                try {
                    //Ensure the line has enough columns before accessing them
                    if (nextLine.length >= 6) {
                        String employeeId = nextLine[0];
                        LocalDate date = LocalDate.parse(nextLine[3], dateFormatter);
                        LocalTime timeIn = parseTimeWithFallbacks(nextLine[4]);
                        LocalTime timeOut = parseTimeWithFallbacks(nextLine[5]);
                        records.add(new Attendance(employeeId, date, timeIn, timeOut));
                    } else {
                        System.err.println("Skipping malformed line in attendance.csv (not enough columns): " + String.join(",", nextLine));
                    }
                } catch (Exception e) {
                    System.err.println("Error parsing line in attendance.csv (will be skipped): " + String.join(",", nextLine) + " - " + e.getMessage());
                }
            }
        } catch (IOException | CsvValidationException e) {
            System.err.println("Error reading attendance file: " + e.getMessage());
            e.printStackTrace();
        }
        return records;
    }


    private LocalTime parseTimeWithFallbacks(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) {
            return null;
        }
        for (DateTimeFormatter formatter : TIME_FORMATTERS) {
            try {
                return LocalTime.parse(timeStr, formatter);
            } catch (DateTimeParseException e) {
                // Ignore and try next formatter
            }
        }
        throw new DateTimeParseException("Time string '" + timeStr + "' could not be parsed with any available format.", timeStr, 0);
    }


    //Saves or updates a single attendance record
    public void saveAttendanceRecord(Attendance recordToSave) {
        List<Attendance> records = getAllAttendanceRecords();
        boolean recordFound = false;
        for (int i = 0; i < records.size(); i++) {
            Attendance record = records.get(i);
            if (record.getEmployeeId().equals(recordToSave.getEmployeeId()) && record.getDate().equals(recordToSave.getDate())) {
                records.set(i, recordToSave);
                recordFound = true;
                break;
            }
        }
        if (!recordFound) {
            records.add(recordToSave);
        }
        // Sort records before saving to maintain a consistent order, e.g., by employee ID then date
        records.sort(Comparator.comparing(Attendance::getEmployeeId).thenComparing(Attendance::getDate));
        saveAllAttendanceRecords(records);
    }
    

    public void recordAttendance(String employeeId, LocalDate date, LocalTime timeIn, LocalTime timeOut) {
        Attendance newAttendanceRecord = new Attendance(employeeId, date, timeIn, timeOut);
        saveAttendanceRecord(newAttendanceRecord);
    }


    //Saves all attendance records to the CSV, overwriting existing content
    private void saveAllAttendanceRecords(List<Attendance> records) {
        try (CSVWriter writer = new CSVWriter(new FileWriter(ATTENDANCE_FILE, false))) { // false to overwrite
            writer.writeNext(ATTENDANCE_HEADER.split(",")); // Write header
            DateTimeFormatter timeFormatterOutput = DateTimeFormatter.ofPattern("HH:mm"); // Standard format for writing time
            DateTimeFormatter dateFormatterOutput = DateTimeFormatter.ofPattern("MM/dd/yyyy"); // Standard format for writing date

            for (Attendance record : records) {
                Employee employee = getEmployeeById(record.getEmployeeId()); // Fetch employee details
                String lastName = "";
                String firstName = "";
                if (employee != null) {
                    lastName = employee.getLastName();
                    firstName = employee.getFirstName();
                } else {
                    // Optionally log or handle cases where employee details are not found for an ID
                    System.err.println("Warning: Employee details not found for ID: " + record.getEmployeeId() + " when saving attendance.");
                }
                writer.writeNext(new String[]{
                    record.getEmployeeId(),
                    lastName, // Last Name
                    firstName, // First Name
                    record.getDate().format(dateFormatterOutput),
                    record.getTimeIn() != null ? record.getTimeIn().format(timeFormatterOutput) : "", // Handle null timeIn
                    record.getTimeOut() != null ? record.getTimeOut().format(timeFormatterOutput) : "" // Handle null timeOut
                });
            }
        } catch (IOException e) {
            System.err.println("Error saving attendance records: " + e.getMessage());
            e.printStackTrace();
        }
    }

    //Retrieves an employee by their ID
    public Employee getEmployeeById(String employeeId) {
        // Make sure readEmployees() correctly uses the Map constructor or handles errors.
        List<Employee> employees = readEmployees();
        if (employees == null) { // Should not happen if readEmployees returns new ArrayList on error
             return null;
        }
        return employees.stream()
                .filter(emp -> emp != null && emp.getEmployeeId().equals(employeeId)) // Added null check for emp
                .findFirst()
                .orElse(null);
    }

    //Updates details of an existing employee in the list and saves it
    public void saveEmployee(Employee employee) {
        List<Employee> employees = readEmployees();
        // Remove old record if exists, then add updated one
        employees.removeIf(emp -> emp.getEmployeeId().equals(employee.getEmployeeId()));
        employees.add(employee);
        // Sort for consistency, e.g., by employee ID
        employees.sort(Comparator.comparing(Employee::getEmployeeId));
        saveAllEmployees(employees);
    }

    //Deletes an employee by ID and updates the CSV
    public boolean deleteEmployee(String id) {
        List<Employee> employees = readEmployees();
        boolean removed = employees.removeIf(emp -> emp.getEmployeeId().equals(id));
        if (removed) {
            saveAllEmployees(employees);
        }
        return removed;
    }

    //Finds an attendance record for a specific employee on a specific date
    public Attendance findAttendanceRecord(String employeeId, LocalDate date) {
        List<Attendance> records = getAllAttendanceRecords();
        return records.stream()
                .filter(r -> r.getEmployeeId().equals(employeeId) && r.getDate().equals(date))
                .findFirst()
                .orElse(null);
    }

    //Returns the path to the attendance CSV file
    public String getAttendanceFilePath() {
        return ATTENDANCE_FILE;
    }

    //Returns the path to the employee CSV file
    public String getEmployeeFilePath() {
        return EMPLOYEE_FILE;
    }

 
    public int getEmployeeCount() {
        List<Employee> employees = readEmployees();
        return employees != null ? employees.size() : 0;
    }
}