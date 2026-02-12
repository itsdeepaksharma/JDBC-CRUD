package org.example;

import java.sql.*;
import java.util.Scanner;
import java.io.InputStream;
import java.util.Properties;

public class Main {

    private static Properties loadConfig() {
        Properties props = new Properties();
        try (InputStream input = Main.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                System.out.println("Sorry, unable to find config.properties");
                System.exit(1);
            }
            props.load(input);
        } catch (Exception e) {
            System.err.println("Error loading configuration: " + e.getMessage());
            System.exit(1);
        }
        return props;
    }

    public static void main(String[] args) {
        Properties config = loadConfig();
        String url = config.getProperty("db.url");
        String username = config.getProperty("db.username");
        String password = config.getProperty("db.password");

        try{
            Class.forName("com.mysql.cj.jdbc.Driver");
        }catch (ClassNotFoundException e) {
            throw new RuntimeException(e.getException());
        }

        Connection connection = null;
        Scanner sc = new Scanner(System.in);

        try {
            connection = DriverManager.getConnection(url, username, password);
            System.out.println("Database connected successfully!");

            while (true) {
                System.out.println("\n=== Student Management System ===");
                System.out.println("1. Add Student");
                System.out.println("2. View All Students");
                System.out.println("3. Update Student");
                System.out.println("4. Delete Student");
                System.out.println("5. Exit");
                System.out.print("Enter your choice: ");

                int choice;
                try {
                    choice = sc.nextInt();
                } catch (Exception e) {
                    System.out.println("Invalid input! Please enter a number.");
                    sc.nextLine();
                    continue;
                }

                switch (choice) {
                    case 1:
                        addStudent(connection, sc);
                        break;
                    case 2:
                        viewAllStudents(connection);
                        break;
                    case 3:
                        updateStudent(connection, sc);
                        break;
                    case 4:
                        deleteStudent(connection, sc);
                        break;
                    case 5:
                        System.out.println("Exiting...");
                        return;
                    default:
                        System.out.println("Invalid choice! Please try again.");
                }
            }

        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            throw new RuntimeException(e);
        } finally {
            try {
                if (connection != null) connection.close();
                System.out.println("Database connection closed.");
            } catch (SQLException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
    }

    private static void addStudent(Connection connection, Scanner sc) {
        try {
            System.out.println("Enter Name : ");
            String name = sc.next();

            if (name == null || name.trim().isEmpty()) {
                System.out.println("Error: Name cannot be empty!");
                return;
            }
            if (name.length() > 100) {
                System.out.println("Error: Name too long (max 100 characters)!");
                return;
            }

            System.out.println("Enter Age : ");
            int age;
            try {
                age = sc.nextInt();
                if (age <= 0 || age > 120) {
                    System.out.println("Error: Age must be between 1 and 120!");
                    return;
                }
            } catch (Exception e) {
                System.out.println("Error: Please enter a valid integer for age!");
                sc.nextLine();
                return;
            }

            System.out.println("Enter Marks : ");
            double marks;
            try {
                marks = sc.nextDouble();
                if (marks < 0 || marks > 100) {
                    System.out.println("Error: Marks must be between 0 and 100!");
                    return;
                }
            } catch (Exception e) {
                System.out.println("Error: Please enter a valid number for marks!");
                sc.nextLine();
                return;
            }

            String query = "INSERT INTO students(name, age, marks) VALUES(?, ?, ?)";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setString(1, name);
                preparedStatement.setInt(2, age);
                preparedStatement.setDouble(3, marks);

                int rows = preparedStatement.executeUpdate();
                if (rows > 0) {
                    System.out.println("Student added successfully!");
                } else {
                    System.out.println("Failed to add student.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error adding student: " + e.getMessage());
        }
    }

    private static void viewAllStudents(Connection connection) {
        try {
            String query = "SELECT * FROM students ORDER BY id";
            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery(query)) {

                System.out.println("\n=== Student List ===");
                System.out.printf("%-5s %-20s %-5s %-10s\n", "ID", "Name", "Age", "Marks");
                System.out.println("----------------------------------------");

                boolean found = false;
                while (resultSet.next()) {
                    found = true;
                    int id = resultSet.getInt("id");
                    String name = resultSet.getString("name");
                    int age = resultSet.getInt("age");
                    double marks = resultSet.getDouble("marks");

                    System.out.printf("%-5d %-20s %-5d %-10.2f\n", id, name, age, marks);
                }

                if (!found) {
                    System.out.println("No students found.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error viewing students: " + e.getMessage());
        }
    }

    private static void updateStudent(Connection connection, Scanner sc) {
        try {
            System.out.print("Enter Student ID to update: ");
            int id;
            try {
                id = sc.nextInt();
            } catch (Exception e) {
                System.out.println("Invalid ID!");
                sc.nextLine();
                return;
            }

            if (!studentExists(connection, id)) {
                System.out.println("Student with ID " + id + " not found!");
                return;
            }

            System.out.print("Enter new Name (or press Enter to keep current): ");
            sc.nextLine();
            String name = sc.nextLine();

            System.out.print("Enter new Age (or 0 to keep current): ");
            int age = sc.nextInt();

            System.out.print("Enter new Marks (or -1 to keep current): ");
            double marks = sc.nextDouble();

            StringBuilder query = new StringBuilder("UPDATE students SET ");
            boolean hasUpdates = false;

            if (!name.trim().isEmpty()) {
                if (name.length() > 100) {
                    System.out.println("Error: Name too long!");
                    return;
                }
                query.append("name = ?");
                hasUpdates = true;
            }

            if (age > 0 && age <= 120) {
                if (hasUpdates) query.append(", ");
                query.append("age = ?");
                hasUpdates = true;
            }

            if (marks >= 0 && marks <= 100) {
                if (hasUpdates) query.append(", ");
                query.append("marks = ?");
                hasUpdates = true;
            }

            if (!hasUpdates) {
                System.out.println("No updates provided!");
                return;
            }

            query.append(" WHERE id = ?");

            try (PreparedStatement preparedStatement = connection.prepareStatement(query.toString())) {
                int paramIndex = 1;

                if (!name.trim().isEmpty()) {
                    preparedStatement.setString(paramIndex++, name);
                }
                if (age > 0 && age <= 120) {
                    preparedStatement.setInt(paramIndex++, age);
                }
                if (marks >= 0 && marks <= 100) {
                    preparedStatement.setDouble(paramIndex++, marks);
                }
                preparedStatement.setInt(paramIndex, id);

                int rows = preparedStatement.executeUpdate();
                if (rows > 0) {
                    System.out.println("Student updated successfully!");
                } else {
                    System.out.println("Failed to update student.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error updating student: " + e.getMessage());
        }
    }

    private static void deleteStudent(Connection connection, Scanner sc) {
        try {
            System.out.print("Enter Student ID to delete: ");
            int id;
            try {
                id = sc.nextInt();
            } catch (Exception e) {
                System.out.println("Invalid ID!");
                sc.nextLine();
                return;
            }

            if (!studentExists(connection, id)) {
                System.out.println("Student with ID " + id + " not found!");
                return;
            }

            System.out.print("Are you sure you want to delete this student? (Y/N): ");
            String confirm = sc.next();

            if (!confirm.equalsIgnoreCase("Y")) {
                System.out.println("Deletion cancelled.");
                return;
            }

            String query = "DELETE FROM students WHERE id = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setInt(1, id);

                int rows = preparedStatement.executeUpdate();
                if (rows > 0) {
                    System.out.println("Student deleted successfully!");
                } else {
                    System.out.println("Failed to delete student.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error deleting student: " + e.getMessage());
        }
    }

    private static boolean studentExists(Connection connection, int id) {
        try {
            String query = "SELECT id FROM students WHERE id = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setInt(1, id);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    return resultSet.next();
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking student existence: " + e.getMessage());
            return false;
        }
    }
    }