# Smart Bus Assistant – Android Application

An Android application for live public bus tracking, schedule discovery, emergency alerts, driver trip sharing, and transit administration, inspired by the **Chalo** bus app.

---

## 1. Technology Stack & Architecture

- **Language:** Java (100% Java activities, models, adapters, services, utilities — **No Kotlin**)
- **UI System:** Android XML layouts (**No Jetpack Compose**, no web/hybrid frameworks)
- **Interactive Maps:** **MapLibre Maps SDK for Android** (`org.maplibre.gl:android-sdk:11.0.0`)
- **Map Data & Tiles:** **OpenStreetMap (OSM)** compatible tile sources (via MapLibre style JSON, e.g., `https://demotiles.maplibre.org/style.json` or custom tile endpoints)
- **Backend & Realtime Sync:** **Firebase Authentication** and **Firebase Realtime Database**
- **Location Engine:** Android Fused Location Provider Client + Foreground Service for continuous background updates
- **Minimum SDK:** Android 7.0 (API level 24)
- **Target / Compile SDK:** Android 14 (API level 34)

---

## 2. Application Modules & Portals

### A. Passenger Portal
1. **User Authentication:** Registration and login via Firebase Auth with role validation.
2. **Passenger Home:** Greeting, quick-action cards, live search bar, and active buses list with real-time updates.
3. **Bus Search:** Source and destination search with autocomplete suggestions, swap feature, and date/time selection.
4. **Bus List & Filtering:** Filter buses by type (**AC**, **Non-AC**, **Express**, or **All**).
5. **Live Map Tracking (MapLibre):**
   - Interactive OpenStreetMap-based vector map.
   - Live bus marker dynamically moving when driver coordinates change in Firebase.
   - GeoJSON LineString rendering of bus route path.
   - Bus stop markers with custom callouts.
   - Camera auto-centering and zoom on selected bus.
   - Bottom sheet displaying bus number, route name, ETA, and driver info.
6. **Digital Ticket / Pass:** View active and expired tickets, purchase/generate digital transit passes with route and fare calculation.
7. **Emergency SOS System:**
   - Large interactive SOS button with pulsing ring animation.
   - Automatically acquires GPS location and broadcasts emergency complaint/alert to transit authority.
   - Quick one-tap direct dialers for Police (**100**) and Emergency (**112**).
   - Share live emergency location via SMS/WhatsApp/share sheet.
8. **Feedback & Complaints:** 5-star rating system with bus selection, text comments, formal complaints reporting, and history view.
9. **Passenger Profile:** View travel stats (trips count, complaints, ratings), edit profile details, support hotline, and secure logout.

---

### B. Driver Portal
1. **Driver Login:** Dedicated driver authentication flow verifying `role == "driver"`.
2. **Driver Dashboard:**
   - On Duty / Off Duty toggle badge.
   - Assigned bus and route details card.
   - Real-time trip status indicator.
   - **Start Trip:** Automatically initializes trip in Firebase, marks bus active, sets driver on-duty, and launches the foreground location service.
   - **End Trip:** Finalizes trip, records end timestamp, marks bus inactive, and terminates background GPS tracking.
   - **Share Live Location:** Toggle live coordinates upload.
3. **Foreground Location Service (`LocationUpdateService`):**
   - Runs in foreground with persistent notification compliant with Android 10+ and Android 14 requirements (`foregroundServiceType="location"`).
   - High-accuracy GPS polling every 5 seconds.
   - Continuously writes `latitude`, `longitude`, `speed`, `heading`, and `timestamp` to `liveLocations/{busId}` in Firebase Realtime Database.
4. **Driver Map (`DriverMapActivity`):**
   - MapLibre map displaying assigned route stops and GeoJSON polyline.
   - Live driver marker showing current bus position and bearing.
5. **Driver Profile:** Displays license number, contact info, assigned bus, and profile management.

---

### C. Admin Portal
1. **Admin Login:** Dedicated authorized access only verifying `role == "admin"`.
2. **Admin Dashboard:**
   - Real-time statistics counters: Total Buses, Active Buses, Total Drivers, Pending Complaints.
   - Quick action grid navigating to all administration modules.
3. **Manage Buses (`ManageBusesActivity`):** Add, edit, toggle active status, and delete buses.
4. **Manage Drivers (`ManageDriversActivity`):** Register drivers, view license numbers, track duty status, and link drivers to buses.
5. **Manage Routes (`ManageRoutesActivity`):** Create bus routes with source, destination, distance, duration, and fare.
6. **Manage Stops (`ManageStopsActivity`):** Add, reorder, and edit stops with latitude/longitude coordinates and address.
7. **Live Bus Fleet Monitor (`AdminLiveMapActivity`):**
   - MapLibre interactive map displaying **all active buses simultaneously**.
   - Real-time marker updates as multiple drivers upload their locations.
   - Bus detail callouts on marker tap.
8. **View Complaints & Feedback (`ViewComplaintsActivity`):**
   - Tabbed view for customer complaints and ratings.
   - Filter complaints by status (*pending*, *in_review*, *resolved*).
   - Admin reply dialog to resolve passenger grievances.

---

## 3. MapLibre & OpenStreetMap Integration

This application exclusively uses **MapLibre Maps SDK for Android** (`org.maplibre.gl:android-sdk:11.0.0`) and contains **zero Google Maps SDK dependencies or API keys**.

- **Reusable Map Controller:** `com.smartbus.app.maps.MapManager` encapsulates camera navigation, bus marker animation, stop pin annotations, and GeoJSON route line layers.
- **Configurable Tile Source:** The default style URL is set in `Constants.MAP_STYLE_URL`:
  ```java
  public static final String MAP_STYLE_URL = "https://demotiles.maplibre.org/style.json";
  ```
- **Alternative Tile Sources:**
  - OpenStreetMap Raster Tiles: Configure a raster tile style pointing to `https://tile.openstreetmap.org/{z}/{x}/{y}.png`.
  - MapTiler / Stadia Maps: Point `MAP_STYLE_URL` to any vector style JSON: `https://api.maptiler.com/maps/streets/style.json?key=YOUR_KEY`.

---

## 4. Firebase Database Structure

The Firebase Realtime Database is structured as follows:

```
SmartBusAssistant/
├── users/
│   └── {uid}/ { email, name, phone, role: "passenger"|"driver"|"admin", profileImage }
├── passengers/
│   └── {uid}/ { uid, name, email, phone, registrationDate }
├── drivers/
│   └── {uid}/ { uid, name, email, phone, licenseNumber, assignedBusId, assignedRouteId, isOnDuty }
├── buses/
│   └── {busId}/ { busId, busNumber, busName, routeId, driverId, totalSeats, busType, isActive, currentLatitude, currentLongitude }
├── routes/
│   └── {routeId}/ { routeId, routeName, sourceStop, destinationStop, totalStops, distanceKm, estimatedDuration, fareAmount, isActive }
├── stops/
│   └── {stopId}/ { stopId, stopName, routeId, stopOrder, latitude, longitude, address }
├── liveLocations/
│   └── {busId}/ { busId, driverId, latitude, longitude, timestamp, tripStatus, speed, heading }
├── trips/
│   └── {tripId}/ { tripId, busId, driverId, routeId, startTime, endTime, status, startLatitude, startLongitude }
├── tickets/
│   └── {ticketId}/ { ticketId, passengerId, busId, routeId, fromStop, toStop, fareAmount, purchaseTime, status, qrCode }
├── feedback/
│   └── {feedbackId}/ { feedbackId, passengerId, busId, driverId, rating, comment, timestamp, isResolved }
└── complaints/
    └── {complaintId}/ { complaintId, passengerId, busId, subject, description, timestamp, status, adminReply }
```

---

## 5. External Configuration Guide

Follow these steps to configure your own Firebase backend and run the project:

### Step 1: Create Firebase Project & Enable Services
1. Go to the [Firebase Console](https://console.firebase.google.com/).
2. Click **Add Project** and name it `smart-bus-assistant`.
3. In the sidebar under **Build**:
   - **Authentication:** Click *Get Started* → Enable **Email/Password** sign-in provider.
   - **Realtime Database:** Click *Create Database* → Choose your region.

### Step 2: Apply Database Security Rules
In the Firebase Console under **Realtime Database** → **Rules**, paste the contents of `c:\madpbl\database.rules.json` and click **Publish**.

### Step 3: Import Sample Transit Data (Optional, Recommended)
In **Realtime Database** → **Data**, click the three vertical dots (menu) → **Import JSON** → select `c:\madpbl\firebase-sample-data.json`. This instantly populates sample buses (`MH-12-AB-1234`, `MH-12-CD-5678`), routes, stops, and initial live coordinates!

### Step 4: Add `google-services.json`
1. In Firebase Console, go to **Project Settings** → **General**.
2. Under *Your apps*, click **Add app** → select **Android**.
3. Set the Android package name to:
   ```
   com.smartbus.app
   ```
4. Download the generated `google-services.json` file.
5. Replace the placeholder file at:
   ```
   c:\madpbl\app\google-services.json
   ```

### Step 5: Android Studio Setup
1. Launch **Android Studio**.
2. Select **Open** and choose the `c:\madpbl` directory.
3. Allow Gradle to sync dependencies.
4. Run the application on an emulator or connected physical Android device.

---

## 6. Project Package Structure

```
c:\madpbl\app\src\main\java\com\smartbus\app\
├── SmartBusApplication.java               // App entry point with Firebase initialization
├── activities\
│   ├── SplashActivity.java                // Animated splash screen with auto-routing
│   ├── RoleSelectionActivity.java         // Card-based portal selector (Passenger/Driver/Admin)
│   ├── passenger\                         // Passenger Portal activities
│   │   ├── PassengerLoginActivity.java
│   │   ├── PassengerRegisterActivity.java
│   │   ├── PassengerHomeActivity.java
│   │   ├── BusSearchActivity.java
│   │   ├── BusListActivity.java
│   │   ├── LiveTrackingActivity.java      // MapLibre live bus tracking
│   │   ├── TicketActivity.java            // Digital transit pass generator & viewer
│   │   ├── SosActivity.java               // Emergency SOS dispatch & location share
│   │   ├── FeedbackActivity.java          // Ratings & grievances
│   │   └── ProfileActivity.java           // Passenger profile & settings
│   ├── driver\                            // Driver Portal activities
│   │   ├── DriverLoginActivity.java
│   │   ├── DriverHomeActivity.java        // Trip start/end & status dashboard
│   │   ├── DriverProfileActivity.java
│   │   └── DriverMapActivity.java         // Driver route navigation on MapLibre
│   └── admin\                             // Admin Portal activities
│       ├── AdminLoginActivity.java
│       ├── AdminDashboardActivity.java    // System overview & metrics
│       ├── ManageBusesActivity.java       // Fleet management (CRUD)
│       ├── ManageDriversActivity.java     // Driver assignments & status
│       ├── ManageRoutesActivity.java      // Route configuration
│       ├── ManageStopsActivity.java       // Transit stop management
│       ├── ViewComplaintsActivity.java    // Grievance resolution & replies
│       └── AdminLiveMapActivity.java      // Multi-bus fleet tracking on MapLibre
├── adapters\                              // RecyclerView adapters
│   ├── BusAdapter.java
│   ├── AdminBusAdapter.java
│   ├── DriverAdapter.java
│   ├── AdminDriverAdapter.java
│   ├── RouteAdapter.java
│   ├── StopAdapter.java
│   ├── DriverStopAdapter.java
│   ├── TicketAdapter.java
│   ├── FeedbackAdapter.java
│   └── ComplaintAdapter.java
├── database\
│   └── FirebaseHelper.java                // Centralized Firebase Realtime DB singleton
├── maps\
│   └── MapManager.java                    // Reusable MapLibre helper class
├── models\                                // Data POJOs
│   ├── User.java
│   ├── Passenger.java
│   ├── Driver.java
│   ├── Bus.java
│   ├── Route.java
│   ├── Stop.java
│   ├── LiveLocation.java
│   ├── Trip.java
│   ├── Ticket.java
│   ├── Feedback.java
│   └── Complaint.java
├── services\
│   └── LocationUpdateService.java         // Android Foreground Service for live GPS updates
└── utils\
    ├── Constants.java
    ├── SharedPrefManager.java
    ├── PermissionUtils.java
    └── GeoJsonUtils.java
```
