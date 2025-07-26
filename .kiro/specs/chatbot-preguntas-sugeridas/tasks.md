# Implementation Plan

- [ ] 1. Fix API service integration to use existing frequent questions endpoint
  - Update ChatbotRepository to use the existing ChatbotApiService.getFrequentQuestions() method
  - Integrate the existing api/chatbot/frequent-questions/ endpoint that already exists in Django
  - Remove duplicate API definitions and consolidate to use existing backend functionality
  - _Requirements: 2.2, 2.3_

- [ ] 2. Implement local caching for frequent questions from existing API
  - Create FrequentQuestionCache data class for caching API responses locally
  - Add SharedPreferences storage for offline access to frequent questions
  - Implement cache expiration logic (24 hours) and refresh mechanisms
  - _Requirements: 5.2, 5.3_

- [ ] 3. Create FrequentQuestionsManager to handle existing API integration
  - Create manager class that uses existing ChatbotApiService.getFrequentQuestions()
  - Implement caching layer over existing API calls
  - Add error handling and fallback to cached data when API fails
  - _Requirements: 2.2, 2.3, 5.4_

- [x] 4. Enhance ChatbotViewModel to always show suggestions using existing API



  - Modify sendMessage method to always display suggestions after bot responses
  - Implement fallback logic: API recommendedQuestions → frequent questions from existing endpoint → default questions
  - Use FrequentQuestionsManager to get cached frequent questions when API response is empty
  - Ensure suggestions appear after every bot response without exception
  - _Requirements: 1.1, 1.2, 1.3, 2.1, 2.2_

- [ ] 5. Add default hardcoded questions as final fallback
  - Create list of 4 default questions in Spanish for emergency fallback
  - Implement logic to show these when both API responses and cache fail
  - Ensure these questions are contextually appropriate for Aquanqa app
  - _Requirements: 1.2, 2.4, 5.4_

- [ ] 6. Update ChatbotRepository to integrate frequent questions from existing API
  - Add method to fetch frequent questions using existing ChatbotApiService
  - Integrate FrequentQuestionsManager for caching and fallback logic
  - Modify postQuery method to ensure suggestions are always available
  - _Requirements: 2.2, 2.3, 5.4_

- [ ] 7. Add comprehensive error handling and fallback mechanisms
  - Implement network error handling with graceful degradation to cached frequent questions
  - Add timeout handling for API calls with fallback to default questions
  - Ensure suggestions always appear even during complete API failures
  - Add logging for debugging suggestion display issues
  - _Requirements: 1.2, 2.4, 5.4_

- [ ] 8. Create unit tests for FrequentQuestionsManager
  - Test integration with existing ChatbotApiService.getFrequentQuestions()
  - Test caching functionality and expiration logic
  - Test error handling and fallback scenarios
  - Test cache invalidation and refresh mechanisms
  - _Requirements: 5.1, 5.2, 5.3_

- [ ] 9. Create unit tests for enhanced ChatbotViewModel
  - Test that suggestions always appear after bot responses
  - Test fallback logic from API recommendations to frequent questions to defaults
  - Test integration with existing ChatbotRepository methods
  - Test error scenarios and graceful degradation
  - _Requirements: 1.1, 1.2, 1.3, 2.1, 2.2_

- [ ] 10. Create integration tests for ChatbotRepository with existing API
  - Test integration between repository and existing ChatbotApiService endpoints
  - Test frequent questions API calls and caching behavior
  - Test error handling and fallback to cached data
  - Test end-to-end suggestion flow from existing Django API to UI
  - _Requirements: 2.2, 2.3, 3.1, 3.2_

- [ ] 11. Create UI tests for suggestion display functionality
  - Test that suggestions appear after every bot response in UI
  - Test suggestion click interactions and proper message sending
  - Test fallback question display during network issues
  - Test that existing ChatAdapter properly handles all suggestion scenarios
  - _Requirements: 1.1, 1.4, 4.1, 4.2, 4.3_