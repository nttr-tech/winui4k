using System.Reflection;
using System.Text.Json;
var result = new Dictionary<string, string>();
foreach (var p in typeof(RibbonSpace.Demo.Cad.CadArt).GetProperties(BindingFlags.Public | BindingFlags.Static))
{
    if (p.PropertyType == typeof(string)) result[p.Name] = (string)p.GetValue(null)!;
}
File.WriteAllText(args[0], JsonSerializer.Serialize(result, new JsonSerializerOptions { WriteIndented = true }));
Console.WriteLine(result.Count);
