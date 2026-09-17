package com.flwolfy.paytp.data.lang;

import java.util.Map;

record PayTpLanguage(String name, Map<String, String> translations) {

  PayTpLanguage {
    translations = Map.copyOf(translations);
  }
}
