/* JLanguageTool, a natural language style checker 
 * Copyright (C) 2005 Daniel Naber (http://www.danielnaber.de)
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301
 * USA
 */
package de.danielnaber.languagetool.rules.patterns;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import de.danielnaber.languagetool.Language;

/**
 * Loads {@link PatternRule}s from an XML file.
 * 
 * @author Daniel Naber
 */
public class PatternRuleLoader extends DefaultHandler {

  private List rules;

  public PatternRuleLoader() {
  }

  public List getRules(String filename) throws ParserConfigurationException, SAXException, IOException {
    PatternRuleHandler handler = new PatternRuleHandler();
    SAXParserFactory factory = SAXParserFactory.newInstance();
    SAXParser saxParser = factory.newSAXParser();
    saxParser.parse(new File(filename), handler);
    rules = handler.getRules();
    return rules;
  }
  
}

class PatternRuleHandler extends DefaultHandler {

  private List rules = new ArrayList();
  private String id;
  private boolean caseSensitive = false;
  private String languageStr;
  private String pattern;
  private String description;
  private String ruleGroupId;
  private String ruleGroupDescription;
  private StringBuffer correctExample = new StringBuffer();
  private StringBuffer incorrectExample = new StringBuffer();
  
  private boolean inRuleGroup = false;
  private boolean inPattern = false;
  private boolean inCorrectExample = false;
  private boolean inIncorrectExample = false;
  
  List getRules() {
    return rules;
  }
  
  //===========================================================
  // SAX DocumentHandler methods
  //===========================================================

  public void startElement(String namespaceURI, String lName, String qName, Attributes attrs) {
    if (namespaceURI == null) namespaceURI = null;      // avoid compiler warning
    if (lName == null) lName = null;      // avoid compiler warning
    if (qName.equals("rule")) {
      id = attrs.getValue("id");
      if (inRuleGroup && id == null)
        id = ruleGroupId;
      description = attrs.getValue("name");
      if (inRuleGroup && description == null)
        description = ruleGroupDescription;
    } else if (qName.equals("pattern")) {
      inPattern = true;
      languageStr = attrs.getValue("lang");
      if (attrs.getValue("case_sensitive") != null && attrs.getValue("case_sensitive").equals("yes"))
        caseSensitive = true;
    } else if (qName.equals("example") && attrs.getValue("type").equals("correct")) {
      inCorrectExample = true;
      correctExample = new StringBuffer();
    } else if (qName.equals("example") && attrs.getValue("type").equals("incorrect")) {
      inIncorrectExample = true;
      incorrectExample = new StringBuffer();
    } else if (qName.equals("rulegroup")) {
      ruleGroupId = attrs.getValue("id");
      ruleGroupDescription = attrs.getValue("name");
      inRuleGroup = true;
    }
  }

  public void endElement(String namespaceURI, String sName, String qName) throws SAXException {
    if (namespaceURI == null) namespaceURI = null;      // avoid compiler warning
    if (sName == null) sName = null;      // avoid compiler warning
    Language language = null;
    if (qName.equals("rule")) {
      language = Language.getLanguageforShortName(languageStr);
      if (language == null) {
        throw new SAXException("Unknown language '" + languageStr + "'");
      }
      PatternRule rule = new PatternRule(id, language, pattern, description);
      rule.setCorrectExample(correctExample.toString());
      rule.setIncorrectExample(incorrectExample.toString());
      rule.setCaseSensitive(caseSensitive);
      rules.add(rule);
    } else if (qName.equals("pattern")) {
      inPattern = false;
    } else if (qName.equals("example")) {
      inCorrectExample = false;
      inIncorrectExample = false;
    } else if (qName.equals("rulegroup")) {
      inRuleGroup = false;
    }
  }

  public void characters(char buf[], int offset, int len) {
    String s = new String(buf, offset, len);
    if (inPattern) {
      pattern = s;
    } else if (inCorrectExample) {
      correctExample.append(s);
    } else if (inIncorrectExample) {
      incorrectExample.append(s);
    }
  }

}
