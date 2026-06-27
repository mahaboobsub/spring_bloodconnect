package com.bloodconnect.service;

import com.bloodconnect.model.User;
import com.bloodconnect.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Optional;

@Component
public class DatabaseInitializerService implements CommandLineRunner {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Override
    public void run(String... args) throws Exception {
        boolean tablesExist = false;
        try {
            jdbcTemplate.execute("SELECT 1 FROM users LIMIT 1");
            tablesExist = true;
            System.out.println("DatabaseInitializerService: 'users' table exists. Skipping schema initialization.");
        } catch (Exception e) {
            System.out.println("DatabaseInitializerService: 'users' table not found. Initializing database schema...");
        }

        if (!tablesExist) {
            try {
                ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
                populator.addScript(new ClassPathResource("schema.sql"));
                DatabasePopulatorUtils.execute(populator, dataSource);
                System.out.println("DatabaseInitializerService: Schema initialization complete.");
            } catch (Exception e) {
                System.err.println("DatabaseInitializerService ERROR: Failed to read/execute schema.sql");
                e.printStackTrace();
            }
        }

        // Seed or verify Admin User
        try {
            Optional<User> adminOpt = userRepository.findByEmail("admin@bloodconnect.com");
            if (adminOpt.isEmpty()) {
                User admin = new User(
                    "System Admin",
                    "admin@bloodconnect.com",
                    "$2a$10$UE6oFLim8BTEoFqO/JW3VeTpt1hMd.lsFeqL2HtVUq2a9krwaRJIq",
                    "9999999999",
                    "ADMIN"
                );
                userRepository.save(admin);
                System.out.println("DatabaseInitializerService: Seeded admin account.");
            } else {
                User admin = adminOpt.get();
                admin.setPasswordHash("$2a$10$UE6oFLim8BTEoFqO/JW3VeTpt1hMd.lsFeqL2HtVUq2a9krwaRJIq");
                userRepository.save(admin);
                System.out.println("DatabaseInitializerService: Admin password hash verified & updated.");
            }
        } catch (Exception e) {
            System.err.println("DatabaseInitializerService: Failed to verify/seed Admin account: " + e.getMessage());
        }
    }
}
