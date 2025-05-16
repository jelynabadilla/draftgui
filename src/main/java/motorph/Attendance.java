package motorph;

import java.time.LocalDate;
import java.time.LocalTime;


/* 
*Represent an attendance record for an employee, 
*including their ID, date of attendance, 
*and the times they cloked in/out.
*/
public class Attendance {
    private String employeeId;
    private LocalDate date;
    private LocalTime timeIn;
    private LocalTime timeOut;

    public Attendance() {}

    /*
    Constructor to create an attendance object with specific values
    */
    public Attendance(String employeeId, LocalDate date, LocalTime timeIn, LocalTime timeOut) {
        this.employeeId = employeeId;
        this.date = date;
        this.timeIn = timeIn;
        this.timeOut = timeOut;
    }

    /* 
    Getters and Setters for employee ID, Date, and clocked in/out 
    */
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    
    public LocalTime getTimeIn() { return timeIn; }
    public void setTimeIn(LocalTime timeIn) { this.timeIn = timeIn; }
    
    public LocalTime getTimeOut() { return timeOut; }
    public void setTimeOut(LocalTime timeOut) { this.timeOut = timeOut; }
}