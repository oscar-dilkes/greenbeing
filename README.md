# Greenbeing: Ecological Momentary Assessment of Green Spaces and Mental Wellbeing

Greenbeing is an innovative research project developed for my Master's thesis (which achieved a mark of 77), focusing on understanding the impact of green spaces on mental wellbeing through Ecological Momentary Assessment (EMA). This project utilises Java for backend development, integrating with APIs such as Twilio for messaging, Google Cloud Vision for image analysis, and AWS S3 for data storage. The project facilitates real-time data collection and analysis on the relationship between individuals' exposure to green spaces and their mental wellbeing, using WhatsApp as a chatbot interface.

Greenbeing reaffirmed existing findings through a bifaceted "greenness analysis" approach, utilising both location data alongside OS maps for a location-based greenness approach, and the use of participant photography alongside Google's cloud vision for an image-based greenness approach.

The thesis is available for viewing in this repository.

## Findings
- The findings contradicted existing research on gender's effect in mediating the relationship between greenspace walking and mental wellbeing, suggesting that the effect was stronger in those who identified as female, whereas previous research indicated that the effect was stronger in those who identify as male.
- A novel finding was uncovered in that workplace greenness mediates the relationship between greenspace walking and mental wellbeing such that the effect is stronger amongst those whose workplaces scored a lower greenness rating.

## Project Overview

This repository contains the Java source code developed for the GreenBeing project, which includes modules for conversation handling, data analysis, image processing, and survey management. The system engages participants through automated conversations on WhatsApp, collects responses, and processes the data to explore the implications of greenness on mental wellbeing.

## Source Files

- `Main.java`: Initialises the application and sets up the server to receive messages from Twilio.
- `MessageSender.java`: Handles the sending of messages and images to participants through Twilio.
- `Participant.java`: Manages participant information and survey responses.
- `Conversation.java`: Manages the state and progression of conversations with participants.
- `ConversationHandler.java`, `NewUserConversationHandler.java`, `SurveyConversationHandler.java`: Abstract base and concrete classes handling different stages of conversation and survey progression.
- `Response.java`: Encapsulates responses from participants, including text responses, media, and location data.
- `SurveyResponse.java`: Stores survey responses for further analysis.
- `DataAnalysisUtils.java`: Provides utilities for data analysis, including image and location analysis.
- `ImageProcessor.java`: Processes images submitted by participants, utilizing Google Cloud Vision API for analysis.
- `ParticipantDataHandler.java`: Manages data related to participants, including storing and retrieving participant information from a spreadsheet.
- `S3ImageUploader.java`: Facilitates uploading images to AWS S3 for storage.
- `SurveyResponseHandler.java`: Handles the logic for survey response analysis and storage.
- `WalkDataHandler.java`: Manages walk data, including writing data to spreadsheets for analysis.

## Installation and Setup

### Prerequisites

- Java JDK 11 or higher
- Maven for dependency management
- Twilio account and API keys
- Google Cloud account with Vision API enabled
- AWS account with S3 access

### Running the Application

1. Clone the repository:
```bash
git clone https://github.com/<username>/greenbeing.git
```
2. Install Dependencies
```bash
cd greenbeing
mvn install
```
3. Set up envirnoment variables with your Twilio, Google Cloud, and AWS credentials.
4. You will also require the doped "participantinformation.xlsx" file which needs to be placed within the same directory as the JAR.
5. Run the application:
```bash
java -jar target/greenbeing.jar
```
