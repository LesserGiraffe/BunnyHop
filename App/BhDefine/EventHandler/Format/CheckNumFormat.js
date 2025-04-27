(function() {
  bhText = String(bhText);
  if (bhText.length > 512) {
    return false;
  }
  if (bhText === 'Infinity' || bhText === '-Infinity' || bhText === 'NaN') {
    return true;
  }
  let decRegex = /^-?(0|[1-9](_?\d)*)(\.\d(_?\d)*)?([eE][-+]?(0|[1-9](_?\d)*))?$/;
  if (decRegex.test(bhText)) {
    bhText = bhText.replaceAll('_', '');
    return Number.isFinite(Number.parseFloat(bhText));
  }

  let hexRegex = /^-?0[xX][\dA-Fa-f](_?[\dA-Fa-f])*$/;
  if (hexRegex.test(bhText)) {
    bhText = bhText.replaceAll(/(_|0x|0X)/g, '');
    return Number.isFinite(Number.parseInt(bhText, 16));
  }

  let binRegex = /^-?0[bB][01](_?[01])*$/;
  if (binRegex.test(bhText)) {
    bhText = bhText.replaceAll(/(_|0b|0B)/g, '');
    return Number.isFinite(Number.parseInt(bhText, 2));
  }
  return false;
})();
