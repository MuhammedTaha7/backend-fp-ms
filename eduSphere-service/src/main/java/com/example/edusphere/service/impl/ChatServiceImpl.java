package com.example.edusphere.service.impl;

import com.example.edusphere.service.ChatService;
import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Service
public class ChatServiceImpl implements ChatService {

    private final Random random = new Random();

    @Override
    public String getBotResponse(String userMessage) {
        String lowerCaseMessage = userMessage.toLowerCase().trim();

        // Greeting responses
        if (containsAny(lowerCaseMessage, Arrays.asList("hello", "hi", "hey", "good morning", "good afternoon", "good evening"))) {
            List<String> greetings = Arrays.asList(
                    "Hello! How can I help you today?",
                    "Hi there! What can I assist you with?",
                    "Hey! I'm here to help you navigate EduSphere. What do you need?",
                    "Good to see you! How can I support your learning journey today?"
            );
            return getRandomResponse(greetings);
        }

        // How are you responses
        if (containsAny(lowerCaseMessage, Arrays.asList("how are you", "how do you do", "what's up", "whats up"))) {
            List<String> responses = Arrays.asList(
                    "I'm doing great, thanks for asking! I'm here and ready to help you with anything related to your studies.",
                    "I'm fantastic! Always excited to help students like you. What can I do for you today?",
                    "I'm doing well! I'm here 24/7 to assist you with courses, assignments, and more. How can I help?",
                    "I'm excellent, thank you! Ready to make your EduSphere experience smoother. What do you need help with?"
            );
            return getRandomResponse(responses);
        }

        // Goodbye responses
        if (containsAny(lowerCaseMessage, Arrays.asList("bye", "goodbye", "see you", "farewell", "take care"))) {
            List<String> goodbyes = Arrays.asList(
                    "Goodbye! Feel free to come back anytime you need help.",
                    "See you later! Good luck with your studies!",
                    "Take care! I'll be here whenever you need assistance.",
                    "Farewell! Have a great day and happy learning!"
            );
            return getRandomResponse(goodbyes);
        }

        // Help responses
        if (containsAny(lowerCaseMessage, Arrays.asList("help", "assist", "support", "what can you do"))) {
            return "I'm your EduSphere assistant! I can help you with:\n" +
                    "📚 Course information and enrollment\n" +
                    "📝 Assignment tracking and deadlines\n" +
                    "📊 Grades and academic performance\n" +
                    "👤 Profile management\n" +
                    "🔍 Navigation tips\n" +
                    "Just ask me about any of these topics!";
        }

        // Course-related responses
        if (containsAny(lowerCaseMessage, Arrays.asList("course", "classes", "subjects", "enrollment", "enroll"))) {
            if (lowerCaseMessage.contains("how") || lowerCaseMessage.contains("enroll")) {
                return "To enroll in courses: Go to the 'Courses' section → Browse available courses → Click 'Enroll' on your desired course. You can also search for specific courses by name or code!";
            }
            return "You can find all available courses in the 'Courses' section. There you can:\n" +
                    "• Browse by category or semester\n" +
                    "• Search by course name or code\n" +
                    "• View course details and prerequisites\n" +
                    "• Enroll in new courses";
        }

        // Assignment-related responses
        if (containsAny(lowerCaseMessage, Arrays.asList("assignment", "homework", "task", "due date", "deadline"))) {
            if (lowerCaseMessage.contains("submit") || lowerCaseMessage.contains("upload")) {
                return "To submit assignments: Go to your course page → Find the assignment → Click 'Submit' → Upload your file or enter your response. Don't forget to check the deadline!";
            }
            return "Your assignments are available on the dashboard! You can:\n" +
                    "• View pending assignments with due dates\n" +
                    "• Check submission status\n" +
                    "• Submit work directly through the platform\n" +
                    "• Get reminders for upcoming deadlines";
        }

        // Grades-related responses
        if (containsAny(lowerCaseMessage, Arrays.asList("grade", "score", "marks", "result", "performance"))) {
            return "To check your grades:\n" +
                    "1. Go to your Profile\n" +
                    "2. Click on the 'Grades' tab\n" +
                    "3. View detailed breakdown by course\n" +
                    "You can also see your GPA and progress tracking there!";
        }

        // Profile-related responses
        if (containsAny(lowerCaseMessage, Arrays.asList("profile", "account", "personal info", "edit profile"))) {
            return "Your profile is your central hub! Here you can:\n" +
                    "• Update personal information\n" +
                    "• View your enrolled courses\n" +
                    "• Check academic performance\n" +
                    "• Manage account settings\n" +
                    "Click on your name in the top-right corner to access it!";
        }

        // Thank you responses
        if (containsAny(lowerCaseMessage, Arrays.asList("thank you", "thanks", "appreciate", "grateful"))) {
            List<String> thanks = Arrays.asList(
                    "You're very welcome! Happy to help anytime!",
                    "My pleasure! That's what I'm here for!",
                    "Glad I could help! Feel free to ask anything else!",
                    "You're welcome! Wishing you success in your studies!"
            );
            return getRandomResponse(thanks);
        }

        // Navigation help
        if (containsAny(lowerCaseMessage, Arrays.asList("navigate", "find", "where", "location", "menu"))) {
            return "Need help finding something? Here's how to navigate EduSphere:\n" +
                    "🏠 Dashboard - Your main hub with recent activity\n" +
                    "📚 Courses - Browse and manage your courses\n" +
                    "📝 Assignments - View and submit assignments\n" +
                    "👤 Profile - Personal info and grades\n" +
                    "Use the menu bar at the top to access these sections!";
        }

        // Technical issues
        if (containsAny(lowerCaseMessage, Arrays.asList("problem", "issue", "error", "bug", "not working", "broken"))) {
            return "Sorry to hear you're experiencing issues! Here are some quick fixes:\n" +
                    "• Try refreshing the page (F5)\n" +
                    "• Clear your browser cache\n" +
                    "• Check your internet connection\n" +
                    "If the problem persists, please contact technical support or try logging out and back in.";
        }

        // Positive expressions
        if (containsAny(lowerCaseMessage, Arrays.asList("great", "awesome", "excellent", "perfect", "amazing"))) {
            return "That's wonderful to hear! I'm glad everything is working well for you. Let me know if you need help with anything else!";
        }

        // Default response with suggestions
        return "I'm not quite sure about that, but I'm here to help! You can ask me about:\n" +
                "• 📚 Courses and enrollment\n" +
                "• 📝 Assignments and deadlines\n" +
                "• 📊 Grades and performance\n" +
                "• 👤 Profile management\n" +
                "• 🔍 How to navigate the platform\n" +
                "\nWhat would you like to know more about?";
    }

    /**
     * Check if the message contains any of the specified keywords
     */
    private boolean containsAny(String message, List<String> keywords) {
        return keywords.stream().anyMatch(message::contains);
    }

    /**
     * Get a random response from a list of responses
     */
    private String getRandomResponse(List<String> responses) {
        return responses.get(random.nextInt(responses.size()));
    }
}