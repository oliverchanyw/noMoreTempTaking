
$(document).ready(function () {
  populateDropdown();
});

function populateDropdown() {
  let dropdown = $('#dropdown');
  dropdown.empty();
  dropdown.append('<option selected="true" disabled>Choose Name</option>');
  dropdown.prop('selectedIndex', 0);

  const url = 'idjson.txt';

  // Populate dropdown with list of provinces
  $.getJSON(url, function (data) {
    $.each(data, function (key, entry) {
      dropdown.append($('<option></option>').attr('value', entry.id).text(entry.name));
    })
  });
}
