(function() {
  if (bhCommon.isTemplateNode(bhThis)) {
    return true;
  }
  return !bhCauseOfDeletion.isOriginalDeleted();
})();
