(function() {
  if (bhUtil.isTemplateNode(bhThis)) {
    return true;
  }
  return !bhCauseOfDeletion.isOriginalDeleted();
})();
