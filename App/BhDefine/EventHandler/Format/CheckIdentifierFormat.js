(function() {
  
  let text = String(bhText);
  if (text.match(/[\/\*]/) !== null) {
    return false;
  }
  if (text.length > 256) {
    return false;
  }
  
  return text !== "";
})();
