(function() {

  let formatted = bhAddedText
    .replace(/０/g, '0').replace(/１/g, '1')
    .replace(/２/g, '2').replace(/３/g, '3')
    .replace(/４/g, '4').replace(/５/g, '5')
    .replace(/６/g, '6').replace(/７/g, '7')
    .replace(/８/g, '8').replace(/９/g, '9')
    .replace(/Ａ/g, 'A').replace(/ａ/g, 'a')
    .replace(/Ｂ/g, 'B').replace(/ｂ/g, 'b')
    .replace(/Ｃ/g, 'C').replace(/ｃ/g, 'c')
    .replace(/Ｄ/g, 'D').replace(/ｄ/g, 'd')
    .replace(/Ｅ/g, 'E').replace(/ｅ/g, 'e')
    .replace(/Ｆ/g, 'F').replace(/ｆ/g, 'f')
    .replace(/Ｇ/g, 'G').replace(/ｇ/g, 'g')
    .replace(/Ｈ/g, 'H').replace(/ｈ/g, 'h')
    .replace(/Ｉ/g, 'I').replace(/ｉ/g, 'i')
    .replace(/Ｊ/g, 'J').replace(/ｊ/g, 'j')
    .replace(/Ｋ/g, 'K').replace(/ｋ/g, 'k')
    .replace(/Ｌ/g, 'L').replace(/ｌ/g, 'l')
    .replace(/Ｍ/g, 'M').replace(/ｍ/g, 'm')
    .replace(/Ｎ/g, 'N').replace(/ｎ/g, 'n')
    .replace(/Ｏ/g, 'O').replace(/ｏ/g, 'o')
    .replace(/Ｐ/g, 'P').replace(/ｐ/g, 'p')
    .replace(/Ｑ/g, 'Q').replace(/ｑ/g, 'q')
    .replace(/Ｒ/g, 'R').replace(/ｒ/g, 'r')
    .replace(/Ｓ/g, 'S').replace(/ｓ/g, 's')
    .replace(/Ｔ/g, 'T').replace(/ｔ/g, 't')
    .replace(/Ｕ/g, 'U').replace(/ｕ/g, 'u')
    .replace(/Ｖ/g, 'V').replace(/ｖ/g, 'v')
    .replace(/Ｗ/g, 'W').replace(/ｗ/g, 'w')
    .replace(/Ｘ/g, 'X').replace(/ｘ/g, 'x')
    .replace(/Ｙ/g, 'Y').replace(/ｙ/g, 'y')
    .replace(/Ｚ/g, 'Z').replace(/ｚ/g, 'z')
    .replace(/．/g, '.');

  return {bhIsWholeTextFormatted: false, bhFormattedText: formatted};
})();
