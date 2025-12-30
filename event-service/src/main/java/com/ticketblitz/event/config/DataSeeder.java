package com.ticketblitz.event.config;

import com.ticketblitz.event.entity.Category;
import com.ticketblitz.event.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    @Override
    public void run(String... args) {
        if (categoryRepository.count() == 0) {
            categoryRepository.save(Category.builder().name("Music").description("Live Concerts").build());
            categoryRepository.save(Category.builder().name("Sports").description("Matches & Games").build());
            categoryRepository.save(Category.builder().name("Technology").description("Conferences & Meetups").build());
            System.out.println("✅ Default Categories Seeded");
        }
    }
}
