package com.example.carworkshopbookingapp.data.remote;
import com.example.carworkshopbookingapp.data.*;
import com.example.carworkshopbookingapp.data.model.*;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.*;
import java.util.*;
public final class AdminRepository {
 static User require(Transaction t,FirebaseFirestore db) throws FirebaseFirestoreException {
  FirebaseUser current=FirebaseAuth.getInstance().getCurrentUser();
  if(current==null)throw new SecurityException("Admin sign-in required");
  User u=t.get(db.collection("users").document(current.getUid())).toObject(User.class);
  if(u==null||!"admin".equals(u.role))throw new SecurityException("Admin sign-in required");u.uid=current.getUid();return u;
 }
 public void verify(Callback<User> cb){RemoteTasks.run(()->{FirebaseFirestore db=FirebaseFirestore.getInstance();return db.runTransaction(t->require(t,db));},v->v,cb);}
 public <T> void list(String collection,Class<T> type,Callback<List<T>> cb){
  verify(new Callback<User>(){public void onError(Exception e){cb.onError(e);}public void onSuccess(User u){
   RemoteTasks.run(()->FirebaseFirestore.getInstance().collection(collection).get(Source.SERVER),snap->{List<T> out=new ArrayList<>();
    for(DocumentSnapshot d:snap){T x=d.toObject(type);if(x instanceof Booking)((Booking)x).id=d.getId();if(x instanceof Workshop)((Workshop)x).id=d.getId();if(x instanceof ServiceItem)((ServiceItem)x).id=d.getId();if(x!=null)out.add(x);}return out;},cb);
  }});
 }
 public void customer(String uid,Callback<User> cb){verify(new Callback<User>(){public void onError(Exception e){cb.onError(e);}public void onSuccess(User u){new UsersRepository().get(uid,cb);}});}
 public void save(String collection,String id,Object value,Callback<Void> cb){
  RemoteTasks.run(()->{FirebaseFirestore db=FirebaseFirestore.getInstance();DocumentReference ref=db.collection(collection).document(id);
   return db.runTransaction(t->{require(t,db);DocumentSnapshot old=t.get(ref);
    if(value instanceof Workshop){Workshop w=(Workshop)value;if(old.exists()){Workshop before=old.toObject(Workshop.class);w.rating=before.rating;w.ratingCount=before.ratingCount;w.ratingSum=before.ratingSum;w.closedDays=before.closedDays;}}
    t.set(ref,value);return null;});},v->null,cb);
 }
 public void delete(String collection,String id,Callback<Void> cb){RemoteTasks.run(()->{FirebaseFirestore db=FirebaseFirestore.getInstance();return db.runTransaction(t->{require(t,db);t.delete(db.collection(collection).document(id));return null;});},v->null,cb);}
 public void status(String id,BookingStatus expected,BookingStatus next,Callback<Void> cb){
  RemoteTasks.run(()->{FirebaseFirestore db=FirebaseFirestore.getInstance();DocumentReference ref=db.collection("bookings").document(id);
   return db.runTransaction(t->{require(t,db);Booking b=t.get(ref).toObject(Booking.class);
    if(b==null||b.status!=expected||!AdminLogic.allows(b.status,next))throw new IllegalStateException("Status changed. Refresh and try again.");
    List<DocumentReference> locks=new ArrayList<>();
    if(next==BookingStatus.CANCELLED)for(long slot:BookingSlots.locks(b.dateTimeMillis,b.durationMin)){DocumentReference lock=db.collection("workshops").document(b.workshopId).collection("slots").document(Long.toString(slot));if(id.equals(t.get(lock).getString("bookingId")))locks.add(lock);}
    t.update(ref,"status",next.name());for(DocumentReference lock:locks)t.delete(lock);return null;});},v->null,cb);
 }
}
