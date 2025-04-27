(function() {
  bhText = String(bhText);
  if (bhText.length > 512) {
    return false;
  }
  let maxCount = 65535;
  let decRegex = /^(0|[1-9](_?\d)*)(\.\d(_?\d)*)?([eE][-+]?(0|[1-9](_?\d)*))?$/;
  if (decRegex.test(bhText)) {
    bhText = bhText.replaceAll('_', '');
    let num = Number.parseFloat(bhText);
    return (0 <= num) && (num <= maxCount);
  }

  let hexRegex = /^0[xX][\dA-Fa-f](_?[\dA-Fa-f])*$/;
  if (hexRegex.test(bhText)) {
    bhText = bhText.replaceAll(/(_|0x|0X)/g, '');
    let num = Number.parseInt(bhText, 16);
    return (0 <= num) && (num <= maxCount);
  }

  let binRegex = /^0[bB][01](_?[01])*$/;
  if (binRegex.test(bhText)) {
    bhText = bhText.replaceAll(/(_|0b|0B)/g, '');
    let num = Number.parseInt(bhText, 2);
    return (0 <= num) && (num <= maxCount);
  }
  return false;
})();
