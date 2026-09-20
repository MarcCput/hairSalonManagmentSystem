# 💇‍♀️ Hair Salon Management System  
A full-stack salon booking and management platform: a **Spring Boot** REST API paired with a **Vue 3** single-page application. Customers book appointments online and pay in store, stylists see their own schedule, and administrators manage every part of the business services, staff, stock, promotions, payments and feedback.

---

## 📖 **Domain**
A specialized hair salon management system built to support daily business operations.  
The system helps manage customer registration, appointment scheduling, stylist availability, service listings, and payment tracking.

It aims to improve efficiency, reduce scheduling conflicts, and deliver a seamless customer experience.

---

## 🛠️ **How it Works**
Registered **Customers** can book appointments for available **Services** based on time slots managed by **Stylists**.  
Salon **Administrators** control and oversee appointments, manage service offerings, view payments, and maintain staff details.

The system ensures the enforcement of important business rules such as:

- Preventing double booking of customers and stylists  
- Ensuring appointments are made only for available stylists  
- Automatically tracking appointment dates, times, and statuses  
- Ensuring payments correspond accurately to provided services  

---

## 🧩 **Entities**
### **Customer**
Represents individuals who can register and book appointments at the salon.

### **Appointment**
Records scheduled sessions between a customer and stylist and tracks date, time, and status.

### **Stylist**
Salon staff responsible for providing services. Each stylist has a name, specialization, and years of experience.

### **Service**
Represents services offered at the salon (e.g., haircut, coloring, treatment) with price and duration.

### **Payment**
Handles recorded payments for completed appointments, including method and amount.

---

## 👥 **Group Members**
**Leader:**  
1. *Marc Kabala* — System Coordination & Architecture  

**Contributors:**  
2. Marc Kabala (230701876) — Customer Entity  
3. Dayyaan Francis (222277343) — Appointment Entity  
4. Will Koeries (240160711) — Stylist Entity  
5. Witcha Francisco (222894822) — Service Entity  
6. Reece Josephs (218152701) — Payment Entity  


---

## 🔧 **Contributing Guidelines**
### **Pull Request Process**
- Create a new branch named after your student number  
- Implement your assigned feature or entity with tests  
- Ensure all tests pass  
- Update system documentation when necessary  
- Create a Pull Request to the `main` branch  
- Request a review from the team leader  
- Apply review changes  
- Merge after approval  

---

## 📌 **Code Review Checklist**
- Adheres to **Builder Pattern**  
- Includes **TDD (Test-Driven Development)** tests  
- Proper package structure  
- Clear author comments included  
- No merge conflicts  
- All tests passing successfully  

---

## 📊 **UML Class Diagram**
<img width="1980" height="1800" alt="assignment1UML" src="https://github.com/user-attachments/assets/860ca28a-36cb-4f49-9b79-9fb5def4e2a6" />
