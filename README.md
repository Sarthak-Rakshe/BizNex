# BizApp

BizApp is a modern business management application designed to streamline operations for small and medium-sized enterprises. It provides features for user management, customer management, product inventory, billing, and credit management, with a secure backend built using Spring Boot and a ReactJS frontend.

## Features
- **User Management:** Register, authenticate, and manage users with role-based access (Admin, Employee).
- **Customer Management:** Add, update, and view customers, including credit tracking.
- **Product Inventory:** Manage products, including CRUD operations and category-based filtering.
- **Billing System:** Create bills with multiple items, manage bill status, manage return bills, and handle credit payments.
- **Credit Management:** Track and manage customer credits and payments.
- Under Development features :
- **Role-Based Access Control:** Secure endpoints for different user roles using JWT authentication.
- **Pagination & Filtering:** Efficiently handle large datasets with pagination and filtering.
- **Modern UI:** Clean, responsive frontend built with ReactJS (see `/front-end/my-app`).

## Tech Stack
- **Backend:** Java, Spring Boot, Spring Security, JPA/Hibernate, MySQL, MapStruct
- **Frontend:** ReactJS, Axios, React Router
- **Build Tools:** Maven
- **Other:** Lombok, JWT, CORS, Validation

## Getting Started

### Prerequisites
- Java 17 or higher
- Maven
- MySQL
- Node.js & npm (for frontend)

### Backend Setup
1. Clone the repository.
2. Configure your MySQL database in `src/main/resources/application.properties`.
3. Build and run the Spring Boot application:
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

### Frontend Setup
1. Navigate to `front-end/my-app`.
2. Install dependencies:
   ```bash
   npm install
   ```
3. Start the React development server:
   ```bash
   npm start
   ```

## API Endpoints
- `/api/v1/users` - User registration, login, and management
- `/api/v1/customers` - Customer CRUD and credit management
- `/api/v1/products` - Product CRUD and search
- `/api/v1/billing` - Bill creation, update, and status management

## Contributing
Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

## License
[MIT](LICENSE)

---

**Note:** This project is under active development. Features and structure may change.
