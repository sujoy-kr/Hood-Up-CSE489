package com.example.carworkshopbookingapp.ui;
import android.widget.*;import android.view.*;import androidx.appcompat.app.AlertDialog;import com.example.carworkshopbookingapp.data.*;import com.example.carworkshopbookingapp.data.model.*;import java.util.*;
public abstract class AdminCatalogActivity extends AdminActivity {
 private android.os.Bundle draft;private final List<EditText> draftFields=new ArrayList<>();private List<String> offered;private Spinner imageField;private String editingId;private boolean editing,existing;
 @Override protected void onCreate(android.os.Bundle state){super.onCreate(state);draft=state==null?null:state.getBundle("catalog_draft");}
 @Override protected void onSaveInstanceState(android.os.Bundle state){if(editing){android.os.Bundle saved=new android.os.Bundle();saved.putString("id",editingId);saved.putBoolean("existing",existing);ArrayList<String> values=new ArrayList<>();for(EditText e:draftFields)values.add(e.getText().toString());saved.putStringArrayList("fields",values);if(offered!=null)saved.putStringArrayList("services",new ArrayList<>(offered));if(imageField!=null)saved.putInt("image",imageField.getSelectedItemPosition());state.putBundle("catalog_draft",saved);}super.onSaveInstanceState(state);}
 @Override protected boolean keepContent(){return editing;}
 protected abstract boolean workshops();
 @Override protected void navigateBack(){if(editing){editing=false;draft=null;reset();ready();}else finish();}
 protected void ready(){if(draft!=null){edit(null);return;}editing=false;header(workshops()?"Workshops":"Services",false,true);primary(workshops()?"Add workshop":"Add service",()->edit(null));
 final TextView pending=loading();
 if(workshops())repo.list("workshops",Workshop.class,new ScreenCallback<List<Workshop>>(){protected void failure(Exception e){pending.setVisibility(View.GONE);fail(e);}protected void success(List<Workshop> xs){pending.setVisibility(View.GONE);show(new ArrayList<Object>(xs));}});
 else repo.list("services",ServiceItem.class,new ScreenCallback<List<ServiceItem>>(){protected void failure(Exception e){pending.setVisibility(View.GONE);fail(e);}protected void success(List<ServiceItem> xs){pending.setVisibility(View.GONE);show(new ArrayList<Object>(xs));}});
 }
 private void show(List<Object> values){if(!alive())return;List<Map<String,Object>> rows=new ArrayList<>();
 for(Object x:values){Map<String,Object> row=new HashMap<>();boolean shop=x instanceof Workshop;
  row.put("icon",shop?com.example.carworkshopbookingapp.R.drawable.ic_store:com.example.carworkshopbookingapp.R.drawable.ic_wrench);
  row.put("title",shop?((Workshop)x).name:((ServiceItem)x).name);
  row.put("subtitle",shop?((Workshop)x).area:com.example.carworkshopbookingapp.util.WorkshopDisplay.money(((ServiceItem)x).basePrice)+" · "+((ServiceItem)x).durationMin+" min");rows.add(row);}
 ListView list=new ListView(this);showList(list);
 list.setAdapter(new SimpleAdapter(this,rows,com.example.carworkshopbookingapp.R.layout.row_admin_entry,new String[]{"icon","title","subtitle"},new int[]{com.example.carworkshopbookingapp.R.id.entry_icon,com.example.carworkshopbookingapp.R.id.entry_title,com.example.carworkshopbookingapp.R.id.entry_subtitle}));list.setOnItemClickListener((a,v,pos,id)->{int index=pos-list.getHeaderViewsCount();if(index>=0&&index<values.size())edit(values.get(index));});if(values.isEmpty())muted("No entries yet. Add one to get started.");}
 private String value(EditText e){return e.getText().toString().trim();}
 private void edit(Object item){reset();editing=true;existing=draft!=null?draft.getBoolean("existing"):item!=null;draftFields.clear();imageField=null;header(workshops()?(existing?"Edit workshop":"New workshop"):(existing?"Edit service":"New service"),false,false);
 Workshop shop=workshops()?(item==null?new Workshop():(Workshop)item):null;ServiceItem service=workshops()?null:(item==null?new ServiceItem():(ServiceItem)item);
 if(workshops())imagePicker(shop);
 String id=draft!=null?draft.getString("id"):item==null?UUID.randomUUID().toString():workshops()?shop.id:service.id;editingId=id;
 EditText name=input("Name",workshops()?shop.name:service.name,1);EditText area=input(workshops()?"Area":"Category",workshops()?shop.area:service.category,1);
 EditText description=input("Description",workshops()?shop.description:service.description,131073);
 EditText address=null,phone=null,open=null,close=null,price=null,duration=null,keywords=null;List<String> selected=new ArrayList<>();offered=selected;
 if(workshops()){address=input("Address",shop.address,1);phone=input("Phone",shop.phone,3);open=timeInput("Opening time",item==null?9:shop.openHour);close=timeInput("Closing time · same as opening means 24 hours",item==null?18:shop.closeHour);if(shop.serviceIds!=null)selected.addAll(shop.serviceIds);
 Button choose=button("Choose services ("+selected.size()+")",()->{});loadServiceChoices(choose,selected);
 }else{price=input("Base price (BDT)",""+service.basePrice,8194);duration=input("Duration (minutes)",""+(item==null?60:service.durationMin),2);keywords=input("Keywords (comma separated)",service.keywords==null?"":android.text.TextUtils.join(", ",service.keywords),1);}
 final EditText a=address,ph=phone,o=open,c=close,pr=price,du=duration,kw=keywords;
 for(int i=0;i<body.getChildCount();i++)if(body.getChildAt(i) instanceof EditText)draftFields.add((EditText)body.getChildAt(i));
 if(draft!=null){ArrayList<String> fields=draft.getStringArrayList("fields");if(fields!=null)for(int i=0;i<Math.min(fields.size(),draftFields.size());i++)draftFields.get(i).setText(fields.get(i));ArrayList<String> ids=draft.getStringArrayList("services");if(ids!=null){selected.clear();selected.addAll(ids);}if(imageField!=null)imageField.setSelection(draft.getInt("image",0));draft=null;}

 primary("Save",()->{try{if(!InputRules.name(value(name))||value(area).isEmpty())throw new IllegalArgumentException("Name and area/category are required.");
 if(workshops()){int oh=com.example.carworkshopbookingapp.util.WorkshopDisplay.parseHour(value(o)),ch=com.example.carworkshopbookingapp.util.WorkshopDisplay.parseHour(value(c));if(oh<0||oh>23||ch<0||ch>23||value(a).isEmpty()||!InputRules.phone(value(ph))||selected.isEmpty())throw new IllegalArgumentException("Enter address, phone, opening/closing times, and choose services.");shop.id=id;shop.name=value(name);shop.area=value(area);shop.address=value(a);shop.phone=value(ph);shop.openHour=oh;shop.closeHour=ch;shop.description=value(description);shop.imageKey=imageField.getSelectedItemPosition()==1?"night":"day";shop.featured=false;shop.serviceIds=new ArrayList<>(selected);
 }else{double amount=Double.parseDouble(value(pr));int minutes=Integer.parseInt(value(du));if(!Double.isFinite(amount)||amount<0||amount>10000000||java.math.BigDecimal.valueOf(amount).scale()>2||minutes<1||minutes>com.example.carworkshopbookingapp.data.BusinessConfig.MAX_SERVICE_DURATION_MIN)throw new IllegalArgumentException("Enter a price with at most two decimals and a valid duration (1–300 minutes).");service.id=id;service.name=value(name);service.category=value(area);service.description=value(description);service.basePrice=amount;service.durationMin=minutes;service.keywords=new ArrayList<>();for(String k:value(kw).split(","))if(!k.trim().isEmpty())service.keywords.add(k.trim().toLowerCase(Locale.ROOT));}
 repo.save(workshops()?"workshops":"services",id,workshops()?shop:service,mutation(()->{editing=false;reset();ready();}));
 }catch(NumberFormatException e){fail(new IllegalArgumentException("Enter valid numeric hours, price and duration."));}catch(IllegalArgumentException e){fail(e);}});
 if(existing)danger("Delete",()->new AlertDialog.Builder(this).setTitle("Delete this entry?").setMessage("Existing booking details are kept. Deleted services will no longer be bookable; update workshop offerings if needed.").setNegativeButton("Keep",null).setPositiveButton("Delete",(d,w)->repo.delete(workshops()?"workshops":"services",id,mutation(()->{editing=false;reset();ready();}))).show());
 }
 private void imagePicker(Workshop shop){
  section("Workshop image");
  muted("Choose the illustration customers will see on this workshop.");
  ImageView preview=new ImageView(this);preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
  preview.setBackgroundResource(com.example.carworkshopbookingapp.R.drawable.bg_card);
  LinearLayout.LayoutParams photoParams=new LinearLayout.LayoutParams(-1,dp(180));photoParams.bottomMargin=dp(16);body.addView(preview,photoParams);
  imageField=spinner("Workshop image",Arrays.asList("Day workshop illustration","Night workshop illustration"));
  imageField.setSelection("night".equals(shop.imageKey)?1:0);
  imageField.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
   public void onItemSelected(AdapterView<?> parent,View view,int position,long id){Workshop chosen=new Workshop();chosen.name="Workshop preview";chosen.imageKey=position==1?"night":"day";com.example.carworkshopbookingapp.util.WorkshopImages.bind(preview,chosen);}
   public void onNothingSelected(AdapterView<?> parent){}
  });
  com.example.carworkshopbookingapp.util.WorkshopImages.bind(preview,shop);
 }
 private EditText timeInput(String label,int hour){
  EditText field=input(label,com.example.carworkshopbookingapp.util.WorkshopDisplay.hour(hour),1);
  field.setFocusable(false);field.setOnClickListener(v->{
   int current=com.example.carworkshopbookingapp.util.WorkshopDisplay.parseHour(value(field));
   new android.app.TimePickerDialog(this,(picker,h,m)->{
    if(m!=0){fail(new IllegalArgumentException("Choose a time on the hour (minutes 00)."));return;}
    field.setText(com.example.carworkshopbookingapp.util.WorkshopDisplay.hour(h));
   },current,0,false).show();
  });return field;
 }
 private void loadServiceChoices(Button choose,List<String> selected){
  choose.setText("Loading services…");choose.setEnabled(false);
  repo.list("services",ServiceItem.class,new ScreenCallback<List<ServiceItem>>(){
   protected void failure(Exception e){fail(e);choose.setEnabled(true);choose.setText("Retry loading services");choose.setOnClickListener(v->loadServiceChoices(choose,selected));}
   protected void success(List<ServiceItem> all){
    choose.setEnabled(true);choose.setText("Choose services ("+selected.size()+")");
    choose.setOnClickListener(v->{if(busy)return;String[] names=new String[all.size()];boolean[] checked=new boolean[all.size()];List<String> draft=new ArrayList<>(selected);
     for(int i=0;i<all.size();i++){names[i]=all.get(i).name;checked[i]=draft.contains(all.get(i).id);}
     if(all.isEmpty()){fail(new IllegalStateException("No services yet. Add a service from Admin first."));return;}
     new AlertDialog.Builder(AdminCatalogActivity.this).setTitle("Offered services").setMultiChoiceItems(names,checked,(d,index,on)->{String key=all.get(index).id;if(on&&!draft.contains(key))draft.add(key);if(!on)draft.remove(key);}).setNegativeButton("Cancel",null).setPositiveButton("Choose",(d,w)->{selected.clear();selected.addAll(draft);choose.setText("Choose services ("+selected.size()+")");}).show();
    });
   }
  });
 }

}
