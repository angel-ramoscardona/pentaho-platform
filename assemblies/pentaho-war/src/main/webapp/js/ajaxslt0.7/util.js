/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/


// Copyright 2005 Google
//
// Author: Steffen Meschkat <mesch@google.com>
//
// Miscellaneous utility and placeholder functions.

// Dummy implmentation for the logging functions. Replace by something
// useful when you want to debug.
function xpathLog(msg) {};
function xsltLog(msg) {};
function xsltLogXml(msg) {};

// Throws an exception if false.
function assert(b) {
  if (!b) {
    throw "Assertion failed";
  }
}

// Splits a string s at all occurrences of character c. This is like
// the split() method of the string object, but IE omits empty
// strings, which violates the invariant (s.split(x).join(x) == s).
function stringSplit(s, c) {
  var a = s.indexOf(c);
  if (a == -1) {
    return [ s ];
  }
  var parts = [];
  parts.push(s.substr(0,a));
  while (a != -1) {
    var a1 = s.indexOf(c, a + 1);
    if (a1 != -1) {
      parts.push(s.substr(a + 1, a1 - a - 1));
    } else {
      parts.push(s.substr(a + 1));
    }
    a = a1;
  }
  return parts;
}

// The following function does what document.importNode(node, true)
// would do for us here; however that method is broken in Safari/1.3,
// so we have to emulate it.
function xmlImportNode(doc, node) {
  if (node.nodeType == DOM_TEXT_NODE) {
    return domCreateTextNode(doc, node.nodeValue);

  } else if (node.nodeType == DOM_CDATA_SECTION_NODE) {
    return domCreateCDATASection(doc, node.nodeValue);

  } else if (node.nodeType == DOM_ELEMENT_NODE) {
    var newNode = domCreateElement(doc, node.nodeName);
    for (var i = 0; i < node.attributes.length; ++i) {
      var an = node.attributes[i];
      var name = an.nodeName;
      var value = an.nodeValue;
      domSetAttribute(newNode, name, value);
    }

    for (var c = node.firstChild; c; c = c.nextSibling) {
      var cn = arguments.callee(doc, c);
      domAppendChild(newNode, cn);
    }

    return newNode;

  } else {
    return domCreateComment(doc, node.nodeName);
  }
}

// A set data structure. It can also be used as a map (i.e. the keys
// can have values other than 1), but we don't call it map because it
// would be ambiguous in this context. Also, the map is iterable, so
// we can use it to replace for-in loops over core javascript Objects.
// For-in iteration breaks when Object.prototype is modified, which
// some clients of the maps API do.
//
// NOTE(mesch): The set keys by the string value of its element, NOT
// by the typed value. In particular, objects can't be used as keys.
//
// @constructor
function Set() {
  this.keys = [];
}

Set.prototype.size = function() {
  return this.keys.length;
}

// Adds the entry to the set, ignoring if it is present.
Set.prototype.add = function(key, opt_value) {
  var value = opt_value || 1;
  if (!this.contains(key)) {
    this[':' + key] = value;
    this.keys.push(key);
  }
}

// Sets the entry in the set, adding if it is not yet present.
Set.prototype.set = function(key, opt_value) {
  var value = opt_value || 1;
  if (!this.contains(key)) {
    this[':' + key] = value;
    this.keys.push(key);
  } else {
    this[':' + key] = value;
  }
}

// Increments the key's value by 1. This works around the fact that
// numbers are always passed by value, never by reference, so that we
// can't increment the value returned by get(), or the iterator
// argument. Sets the key's value to 1 if it doesn't exist yet.
Set.prototype.inc = function(key) {
  if (!this.contains(key)) {
    this[':' + key] = 1;
    this.keys.push(key);
  } else {
    this[':' + key]++;
  }
}

Set.prototype.get = function(key) {
  if (this.contains(key)) {
    return this[':' + key];
  } else {
    var undefined;
    return undefined;
  }
}

// Removes the entry from the set.
Set.prototype.remove = function(key) {
  if (this.contains(key)) {
    delete this[':' + key];
    removeFromArray(this.keys, key, true);
  }
}

// Tests if an entry is in the set.
Set.prototype.contains = function(entry) {
  return typeof this[':' + entry] != 'undefined';
}

// Gets a list of values in the set.
Set.prototype.items = function() {
  var list = [];
  for (var i = 0; i < this.keys.length; ++i) {
    var k = this.keys[i];
    var v = this[':' + k];
    list.push(v);
  }
  return list;
}


// Invokes function f for every key value pair in the set as a method
// of the set.
Set.prototype.map = function(f) {
  for (var i = 0; i < this.keys.length; ++i) {
    var k = this.keys[i];
    f.call(this, k, this[':' + k]);
  }
}

Set.prototype.clear = function() {
  for (var i = 0; i < this.keys.length; ++i) {
    delete this[':' + this.keys[i]];
  }
  this.keys.length = 0;
}


// Applies the given function to each element of the array, preserving
// this, and passing the index.
function mapExec(array, func) {
  for (var i = 0; i < array.length; ++i) {
    func.call(this, array[i], i);
  }
}

// Returns an array that contains the return value of the given
// function applied to every element of the input array.
function mapExpr(array, func) {
  var ret = [];
  for (var i = 0; i < array.length; ++i) {
    ret.push(func(array[i]));
  }
  return ret;
};

// Reverses the given array in place.
function reverseInplace(array) {
  for (var i = 0; i < array.length / 2; ++i) {
    var h = array[i];
    var ii = array.length - i - 1;
    array[i] = array[ii];
    array[ii] = h;
  }
}

// Removes value from array. Returns the number of instances of value
// that were removed from array.
function removeFromArray(array, value, opt_notype) {
  var shift = 0;
  for (var i = 0; i < array.length; ++i) {
    if (array[i] === value || (opt_notype && array[i] == value)) {
      array.splice(i--, 1);
      shift++;
    }
  }
  return shift;
}

// Shallow-copies an array.
function copyArray(dst, src) {
  for (var i = 0; i < src.length; ++i) {
    dst.push(src[i]);
  }
}

// Returns the text value of a node; for nodes without children this
// is the nodeValue, for nodes with children this is the concatenation
// of the value of all children.
function xmlValue(node) {
  if (!node) {
    return '';
  }

  var ret = '';
  if (node.nodeType == DOM_TEXT_NODE ||
      node.nodeType == DOM_CDATA_SECTION_NODE ||
      node.nodeType == DOM_ATTRIBUTE_NODE) {
    ret += node.nodeValue;

  } else if (node.nodeType == DOM_ELEMENT_NODE ||
             node.nodeType == DOM_DOCUMENT_NODE ||
             node.nodeType == DOM_DOCUMENT_FRAGMENT_NODE) {
    for (var i = 0; i < node.childNodes.length; ++i) {
      ret += arguments.callee(node.childNodes[i]);
    }
  }
  return ret;
}

// Returns the representation of a node as XML text.
function xmlText(node, opt_cdata) {
  var buf = [];
  xmlTextR(node, buf, opt_cdata);
  return buf.join('');
}

function xmlTextR(node, buf, cdata) {
  if (node.nodeType == DOM_TEXT_NODE) {
    buf.push(xmlEscapeText(node.nodeValue));

  } else if (node.nodeType == DOM_CDATA_SECTION_NODE) {
    if (cdata) {
      buf.push(node.nodeValue);
    } else {
      buf.push('<![CDATA[' + node.nodeValue + ']]>');
    }

  } else if (node.nodeType == DOM_COMMENT_NODE) {
    buf.push('<!--' + node.nodeValue + '-->');

  } else if (node.nodeType == DOM_ELEMENT_NODE) {
    buf.push('<' + xmlFullNodeName(node));
    for (var i = 0; i < node.attributes.length; ++i) {
      var a = node.attributes[i];
      if (a && a.nodeName && a.nodeValue) {
        buf.push(' ' + xmlFullNodeName(a) + '="' +
                 xmlEscapeAttr(a.nodeValue) + '"');
      }
    }

    if (node.childNodes.length == 0) {
      buf.push('/>');
    } else {
      buf.push('>');
      for (var i = 0; i < node.childNodes.length; ++i) {
        arguments.callee(node.childNodes[i], buf, cdata);
      }
      buf.push('</' + xmlFullNodeName(node) + '>');
    }

  } else if (node.nodeType == DOM_DOCUMENT_NODE ||
             node.nodeType == DOM_DOCUMENT_FRAGMENT_NODE) {
    for (var i = 0; i < node.childNodes.length; ++i) {
      arguments.callee(node.childNodes[i], buf, cdata);
    }
  }
}

function xmlFullNodeName(n) {
  if (n.prefix && n.nodeName.indexOf(n.prefix + ':') != 0) {
    return n.prefix + ':' + n.nodeName;
  } else {
    return n.nodeName;
  }
}

// Escape XML special markup chracters: tag delimiter < > and entity
// reference start delimiter &. The escaped string can be used in XML
// text portions (i.e. between tags).
function xmlEscapeText(s) {
  return ('' + s).replace(/&/g, '&amp;').replace(/</g, '&lt;').
    replace(/>/g, '&gt;');
}

// Escape XML special markup characters: tag delimiter < > entity
// reference start delimiter & and quotes ". The escaped string can be
// used in double quoted XML attribute value portions (i.e. in
// attributes within start tags).
function xmlEscapeAttr(s) {
  return xmlEscapeText(s).replace(/\"/g, '&quot;');
}

// Escape markup in XML text, but don't touch entity references. The
// escaped string can be used as XML text (i.e. between tags).
function xmlEscapeTags(s) {
  return s.replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

/**
 * Wrapper function to access the owner document uniformly for document
 * and other nodes: for the document node, the owner document is the
 * node itself, for all others it's the ownerDocument property.
 *
 * @param {Node} node
 * @return {Document}
 */
function xmlOwnerDocument(node) {
  if (node.nodeType == DOM_DOCUMENT_NODE) {
    return node;
  } else {
    return node.ownerDocument;
  }
}

// Wrapper around DOM methods so we can condense their invocations.
function domGetAttribute(node, name) {
  return node.getAttribute(name);
}

function domSetAttribute(node, name, value) {
  return node.setAttribute(name, value);
}

function domRemoveAttribute(node, name) {
  return node.removeAttribute(name);
}

function domAppendChild(node, child) {
  return node.appendChild(child);
}

function domRemoveChild(node, child) {
  return node.removeChild(child);
}

function domReplaceChild(node, newChild, oldChild) {
  return node.replaceChild(newChild, oldChild);
}

function domInsertBefore(node, newChild, oldChild) {
  return node.insertBefore(newChild, oldChild);
}

function domRemoveNode(node) {
  return domRemoveChild(node.parentNode, node);
}

function domCreateTextNode(doc, text) {
  return doc.createTextNode(text);
}

function domCreateElement(doc, name) {
  return doc.createElement(name);
}

function domCreateAttribute(doc, name) {
  return doc.createAttribute(name);
}

function domCreateCDATASection(doc, data) {
  return doc.createCDATASection(data);
}

function domCreateComment(doc, text) {
  return doc.createComment(text);
}

function domCreateDocumentFragment(doc) {
  return doc.createDocumentFragment();
}

function domGetElementById(doc, id) {
  return doc.getElementById(id);
}

// Same for window methods.
function windowSetInterval(win, fun, time) {
  return win.setInterval(fun, time);
}

function windowClearInterval(win, id) {
  return win.clearInterval(id);
}
