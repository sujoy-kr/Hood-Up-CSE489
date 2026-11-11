package com.example.carworkshopbookingapp.data;
import org.junit.Test;
import static org.junit.Assert.*;
import com.example.carworkshopbookingapp.data.model.*;
import java.util.*;
public class AdminLogicTest {
 @Test public void onlyExplicitForwardTransitionsAreAllowed(){
  int allowed=0;for(BookingStatus from:BookingStatus.values())for(BookingStatus to:BookingStatus.values())if(AdminLogic.allows(from,to))allowed++;
  assertEquals(6,allowed);assertTrue(AdminLogic.allows(BookingStatus.PENDING,BookingStatus.CANCELLED));assertTrue(AdminLogic.allows(BookingStatus.READY,BookingStatus.COMPLETED));
  assertFalse(AdminLogic.allows(BookingStatus.PENDING,BookingStatus.COMPLETED));assertFalse(AdminLogic.allows(BookingStatus.IN_PROGRESS,BookingStatus.CANCELLED));assertFalse(AdminLogic.allows(BookingStatus.COMPLETED,BookingStatus.CONFIRMED));
 }
 @Test public void statsUseDhakaDatesAndCompletedCommissionOnly(){
  Calendar c=Calendar.getInstance(TimeZone.getTimeZone("Asia/Dhaka"));c.clear();c.set(2026,Calendar.JANUARY,1,1,0);long now=c.getTimeInMillis();
  Booking today=new Booking();today.status=BookingStatus.COMPLETED;today.dateTimeMillis=now;today.commission=95;
  Booking yesterday=new Booking();yesterday.status=BookingStatus.COMPLETED;yesterday.dateTimeMillis=now-86400000L;yesterday.commission=100;
  Booking pending=new Booking();pending.status=BookingStatus.PENDING;pending.dateTimeMillis=now;pending.commission=50;
  AdminLogic.Stats s=AdminLogic.stats(Arrays.asList(today,yesterday,pending),now);
  assertEquals(2,s.today);assertEquals(1,s.pending);assertEquals(1,s.month);assertEquals(95,s.commission,0.001);assertEquals(2,s.days[6]);assertEquals(1,s.days[5]);
 }
 @Test public void transitionMatrixMatchesEveryPermittedEdge(){
  Map<BookingStatus,Set<BookingStatus>> expected=new EnumMap<>(BookingStatus.class);
  expected.put(BookingStatus.PENDING,EnumSet.of(BookingStatus.CONFIRMED,BookingStatus.CANCELLED));
  expected.put(BookingStatus.CONFIRMED,EnumSet.of(BookingStatus.IN_PROGRESS,BookingStatus.CANCELLED));
  expected.put(BookingStatus.IN_PROGRESS,EnumSet.of(BookingStatus.READY));
  expected.put(BookingStatus.READY,EnumSet.of(BookingStatus.COMPLETED));
  expected.put(BookingStatus.COMPLETED,EnumSet.noneOf(BookingStatus.class));
  expected.put(BookingStatus.CANCELLED,EnumSet.noneOf(BookingStatus.class));
  for(BookingStatus from:BookingStatus.values())for(BookingStatus to:BookingStatus.values())
   assertEquals(from+" -> "+to,expected.get(from).contains(to),AdminLogic.allows(from,to));
  assertFalse(AdminLogic.allows(null,BookingStatus.PENDING));assertFalse(AdminLogic.allows(BookingStatus.PENDING,null));
 }
}
