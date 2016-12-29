package com.grenslair.ggasm.test;

import static org.junit.Assert.*;

import org.junit.Test;

import com.grenslair.glulx.ggasm.Assemble;

public class AssembleTest {

  @Test
  public void testIsIdentifier_FirstChar() {
      assertTrue(Assemble.isIdentifier('_', true));
      assertTrue(Assemble.isIdentifier('#', true));
      assertTrue(Assemble.isIdentifier('*', true));
      assertTrue(Assemble.isIdentifier('a', true));
      assertTrue(Assemble.isIdentifier('t', true));
      assertTrue(Assemble.isIdentifier('m', true));

      assertFalse(Assemble.isIdentifier(' ', true));
      assertFalse(Assemble.isIdentifier('\t', true));
      assertFalse(Assemble.isIdentifier('3', true));
      assertFalse(Assemble.isIdentifier('^', true));
      assertFalse(Assemble.isIdentifier('+', true));
  }
  @Test
  public void testIsIdentifier_LaterChars() {
      assertTrue(Assemble.isIdentifier('_', false));
      assertTrue(Assemble.isIdentifier('a', false));
      assertTrue(Assemble.isIdentifier('t', false));
      assertTrue(Assemble.isIdentifier('3', false));
      assertTrue(Assemble.isIdentifier('m', false));

      assertFalse(Assemble.isIdentifier('#', false));
      assertFalse(Assemble.isIdentifier('*', false));
      assertFalse(Assemble.isIdentifier(' ', false));
      assertFalse(Assemble.isIdentifier('\t', false));
      assertFalse(Assemble.isIdentifier('^', false));
      assertFalse(Assemble.isIdentifier('+', false));
  }
}
