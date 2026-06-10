# AgriRent - Modern Agriculture Solutions 🚜🌾

AgriRent is a comprehensive digital platform designed to empower farmers and agricultural entrepreneurs. It facilitates the renting of modern agricultural equipment and the direct purchase of high-quality farming products, bridging the gap between resource availability and agricultural needs.

![AgriRent Banner](docs/images/banner.png)

## 🌟 Key Features

### 🚜 Equipment Rental (Farmer to Farmer / Owner to Farmer)
- **Extensive Catalog**: Browse through a wide range of tractors, harvesters, and specialized tools.
- **Availability Scheduling**: Book equipment for specific dates with a real-time availability calendar.
- **Infinite Scrolling**: Smoothly explore equipment listings with an optimized mobile interface.
- **Rating & Reviews**: Built-in feedback system to ensure quality and reliability.

### 🛒 Product Marketplace
- **Direct Selling**: Purchase seeds, fertilizers, and tools directly from verified sellers.
- **Inventory Management**: Real-time stock tracking for products.
- **Order Tracking**: Comprehensive order management from placement to delivery.

### 📊 Admin & Seller Dashboards
- **Management Portal**: Robust web dashboard for admins to manage users, equipment, and products.
- **Real-time Analytics**: Visualization of sales, rentals, and user growth.
- **Subscription Plans**: Premium plans for sellers and owners to unlock advanced features.

---

## 🛠️ Technology Stack

| Layer | Technology |
| :--- | :--- |
| **Backend API** | .NET 8 Web API, Entity Framework Core, SQL Server |
| **Web Dashboard** | ASP.NET Core MVC, Vanilla CSS, JavaScript |
| **Mobile App** | Native Android (Java), Retrofit2, Material Design |
| **Payments** | Razorpay Integration |
| **Authentication** | JWT (JSON Web Tokens) with OTP support |
| **Services** | Firebase Cloud Messaging (FCM) for Notifications |

---

## 📂 Project Documents & Release
- 📄 **[Software Requirements Specification (SRS)](docs/SRS_AgriRent.docx)**
- 📱 **[Download AgriRent APK (v1.0)](release/AgriRent.apk)**

---

## 🚀 Getting Started

### Prerequisites
- [.NET 8 SDK](https://dotnet.microsoft.com/download/dotnet/8.0)
- [Android Studio](https://developer.android.com/studio) (for mobile app)
- SQL Server (LocalDB or Express)

### 1. Setup Backend API
```powershell
cd Api/AgriRent
dotnet restore
dotnet run
```
*API will be available at: `http://localhost:5288`*

### 2. Setup Web Dashboard
```powershell
cd AgriRent.Web
dotnet restore
dotnet run
```
*Web dashboard will be available at: `http://localhost:5186`*

### 3. Setup Android App
1. Open the `App/AgriRent2` folder in Android Studio.
2. Update the `BASE_URL` in `ApiClient.java` to point to your running API.
3. Build and run on an emulator or physical device.

---

## 📸 Full Application Gallery

<p align="center">
  <img src="docs/images/image17.png" width="180"> <img src="docs/images/image18.png" width="180"> <img src="docs/images/image19.png" width="180"> <img src="docs/images/image20.png" width="180">
</p>
<p align="center">
  <img src="docs/images/image21.png" width="180"> <img src="docs/images/image22.png" width="180"> <img src="docs/images/image23.png" width="180"> <img src="docs/images/image24.png" width="180">
</p>
<p align="center">
  <img src="docs/images/image25.png" width="180"> <img src="docs/images/image26.png" width="180"> <img src="docs/images/image27.png" width="180"> <img src="docs/images/image28.png" width="180">
</p>
<p align="center">
  <img src="docs/images/image29.png" width="180"> <img src="docs/images/image30.png" width="180"> <img src="docs/images/image31.png" width="180"> <img src="docs/images/image32.png" width="180">
</p>
<p align="center">
  <img src="docs/images/image33.png" width="180"> <img src="docs/images/image34.png" width="180"> <img src="docs/images/image35.png" width="180"> <img src="docs/images/image36.png" width="180">
</p>
<p align="center">
  <img src="docs/images/image37.png" width="180"> <img src="docs/images/image38.png" width="180"> <img src="docs/images/image39.png" width="180"> <img src="docs/images/image40.png" width="180">
</p>
<p align="center">
  <img src="docs/images/image41.png" width="180"> <img src="docs/images/image42.png" width="180"> <img src="docs/images/image43.png" width="180"> <img src="docs/images/image44.png" width="180">
</p>
<p align="center">
  <img src="docs/images/image45.png" width="180"> <img src="docs/images/image46.png" width="180"> <img src="docs/images/image47.png" width="180"> <img src="docs/images/image48.png" width="180">
</p>
<p align="center">
  <img src="docs/images/image49.png" width="180"> <img src="docs/images/image50.png" width="180"> <img src="docs/images/image51.png" width="180"> <img src="docs/images/image52.png" width="180">
</p>
<p align="center">
  <img src="docs/images/image53.png" width="180"> <img src="docs/images/image54.png" width="180"> <img src="docs/images/image56.png" width="180"> <img src="docs/images/image57.png" width="180">
</p>
<p align="center">
  <img src="docs/images/image58.png" width="180"> <img src="docs/images/image59.png" width="180"> <img src="docs/images/image60.png" width="180"> <img src="docs/images/image61.png" width="180">
</p>
<p align="center">
  <img src="docs/images/image62.png" width="180"> <img src="docs/images/image63.png" width="180"> <img src="docs/images/image64.png" width="180"> <img src="docs/images/image65.png" width="180">
</p>
<p align="center">
  <img src="docs/images/image66.png" width="180"> <img src="docs/images/image67.png" width="180"> <img src="docs/images/image68.png" width="180"> <img src="docs/images/image69.png" width="180">
</p>
<p align="center">
  <img src="docs/images/image70.png" width="180"> <img src="docs/images/image71.png" width="180"> <img src="docs/images/image72.png" width="180"> <img src="docs/images/image73.png" width="180">
</p>
<p align="center">
  <img src="docs/images/image74.png" width="180"> <img src="docs/images/image75.png" width="180"> <img src="docs/images/image76.png" width="180"> <img src="docs/images/image77.png" width="180">
</p>
<p align="center">
  <img src="docs/images/image78.png" width="180"> <img src="docs/images/image79.png" width="180"> <img src="docs/images/image80.png" width="180"> <img src="docs/images/image81.png" width="180">
</p>
<p align="center">
  <img src="docs/images/image82.png" width="180"> <img src="docs/images/image83.png" width="180"> <img src="docs/images/image84.png" width="180"> <img src="docs/images/image85.png" width="180">
</p>
<p align="center">
  <img src="docs/images/image86.png" width="180"> <img src="docs/images/image87.png" width="180"> <img src="docs/images/image88.png" width="180"> <img src="docs/images/image89.png" width="180">
</p>
<p align="center">
  <img src="docs/images/image90.png" width="180"> <img src="docs/images/image91.png" width="180"> <img src="docs/images/image92.png" width="180"> <img src="docs/images/image93.png" width="180">
</p>
<p align="center">
  <img src="docs/images/image94.png" width="180"> <img src="docs/images/image95.png" width="180">
</p>

---

## 👥 Group Members (Contributors)
We are a team of passionate developers dedicated to improving the lives of farmers through technology.
- **Harmish Kachhadiya**
- **Darshak Vaghamshi**
- **Jainish Patel**

---

## 📄 License
This project is licensed under the MIT License.
