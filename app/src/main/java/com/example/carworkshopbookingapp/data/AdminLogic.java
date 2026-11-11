package com.example.carworkshopbookingapp.data;
import com.example.carworkshopbookingapp.data.model.*;
import java.util.*;
public final class AdminLogic {
 private AdminLogic() {}
 public static boolean allows(BookingStatus from, BookingStatus to) {
  if(from==null||to==null)return false;
  switch(from){case PENDING:return to==BookingStatus.CONFIRMED||to==BookingStatus.CANCELLED;
   case CONFIRMED:return to==BookingStatus.IN_PROGRESS||to==BookingStatus.CANCELLED;
   case IN_PROGRESS:return to==BookingStatus.READY; case READY:return to==BookingStatus.COMPLETED; default:return false;}
 }
 public static final class Stats { public int today,pending,month; public double commission; public int[] days=new int[7]; }
 public static Stats stats(List<Booking> bookings,long now){
  Stats s=new Stats(); Calendar c=Calendar.getInstance(TimeZone.getTimeZone("Asia/Dhaka"));c.setTimeInMillis(now);
  int month=c.get(Calendar.MONTH),year=c.get(Calendar.YEAR); c.set(Calendar.HOUR_OF_DAY,0);c.set(Calendar.MINUTE,0);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);
  long start=c.getTimeInMillis(),day=86400000L;
  for(Booking b:bookings){ if(b.dateTimeMillis>=start&&b.dateTimeMillis<start+day)s.today++;
   if(b.status==BookingStatus.PENDING)s.pending++;
   c.setTimeInMillis(b.dateTimeMillis);
   if(b.status==BookingStatus.COMPLETED&&c.get(Calendar.MONTH)==month&&c.get(Calendar.YEAR)==year){s.month++;s.commission+=b.commission;}
   long index=Math.floorDiv(b.dateTimeMillis-(start-6*day),day);if(index>=0&&index<7)s.days[(int)index]++;
  } return s;
 }
}
