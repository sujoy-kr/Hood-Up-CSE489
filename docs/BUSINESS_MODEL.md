# Hood Up business model

Hood Up records a 10% commission on each booking. Customers pay the listed service price, rounded to two decimal places. Commission is calculated on that full price and appears in admin booking details; dashboard revenue counts completed bookings. A ৳1,000 service records ৳100 commission and an illustrative ৳900 workshop share. No real settlement or payment processing is performed by this course demo.

New bookings have no subscription, discount, pickup request or priority eligibility. Every customer can reserve any unoccupied hour, including the first opening hour. Reminders use the same two-hour interval for everyone. Home sorts by rating and name and has no Featured filter or badges.

The booking transaction recomputes the price and commission from the current service record. Firestore rules reject discounted or benefit-bearing new reservations. Old remote bookings retain their original amounts and legacy snapshot fields; cancellation, status transitions and rating remain supported. Previously queued requests with obsolete benefits or changed prices must be cancelled and rebooked rather than silently charged a different amount.

Profile offers contact details, cars, bookings, reminder settings, About and Logout. Profile contact email is separate from the Firebase Authentication sign-in email. Stored legacy account fields remain for upgrade compatibility but provide no benefits and cannot be activated or extended by clients.

Workshop photos are bundled illustrative assets selected by an `imageKey`, so they remain available offline. Equal opening and closing hours represent continuous 24-hour operation; all-day bookings may finish after midnight when the following opening day is not closed.
