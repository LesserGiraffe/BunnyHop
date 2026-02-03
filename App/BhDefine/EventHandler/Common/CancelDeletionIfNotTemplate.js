(function() {
  if (bhThis.isTemplate()) {
    return true;
  }
  return !bhCauseOfDeletion.isOriginalDeleted();
})();
