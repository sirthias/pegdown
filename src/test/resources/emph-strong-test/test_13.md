Blaa

**SELECT @default_late_fees =
     CASE @default_days_out % @default_product_price_days_out
               <br>WHEN 0 <br>THEN 