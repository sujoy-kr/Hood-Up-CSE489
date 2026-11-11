package com.example.carworkshopbookingapp.data;
import org.junit.Test;
import static org.junit.Assert.*;
public class InputRulesTest {
 @Test public void phoneAcceptsFormattedNumbersButRejectsLettersAndBadLengths(){
  assertTrue(InputRules.phone("  +880 (1712) 345-678  "));
  assertFalse(InputRules.phone("abc01712345678"));assertFalse(InputRules.phone("123456"));assertFalse(InputRules.phone("1234567890123456"));assertFalse(InputRules.phone(null));assertFalse(InputRules.phone("++8801712345678"));
 }
 @Test public void nameTrimsAndBoundsInput(){assertTrue(InputRules.name("  Ayesha Rahman "));assertFalse(InputRules.name("   "));assertFalse(InputRules.name(" A "));assertFalse(InputRules.name(new String(new char[101]).replace('\0','a')));}
}
